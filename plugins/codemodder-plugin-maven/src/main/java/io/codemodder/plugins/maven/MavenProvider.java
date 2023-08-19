package io.codemodder.plugins.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import io.codemodder.ProjectProvider;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.POMDocument;
import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.POMScanner;
import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.ProjectModelFactory;
import io.github.pixee.maven.operator.QueryType;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Maven dependency management functions to codemods.
 *
 * <p>Arshan asked me to add some more docs. Here they are in order to bring more context:
 *
 * <p>Current Limitations are:
 *
 * <p>a. We skip parent finding if there's not a relativePath declaration (this is by design), so
 * sometimes pom finding will fail on purpose b. there are several flags on ProjectModelFactory
 * which aren't applied. They relate to verisons, upgrading and particularly: Actives Profiles c. If
 * you need anything declared in a ~/.m2/settings.xml, we don't support that (e.g., passwords or
 * proxies) d. Haven't tested, but I'm almost sure that it wouldn't work on any repo other than
 * central e. We allow on this module to do online resolution. HOWEVER by default its offline f. You
 * need to set an `M2_REPO` environment variable and/or property or declare withRepositoryPath to
 * somewhere writable
 */
public final class MavenProvider implements ProjectProvider {

  /** Represents a failure when doing a dependency update. */
  static class DependencyUpdateException extends RuntimeException {
    private DependencyUpdateException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** A seam for handling writing poms to disk. */
  interface PomModifier {
    /**
     * Modifies a POM writing back its contents
     *
     * @param path where to write
     * @param contents contents to write
     * @throws IOException failure when writing
     */
    void modify(final Path path, final byte[] contents) throws IOException;
  }

  /** Default Implementation of Pom Modifier Interface */
  static class DefaultPomModifier implements PomModifier {
    @Override
    public void modify(final Path path, final byte[] contents) throws IOException {
      Files.write(path, contents);
    }
  }

  private final boolean offline;

  private final PomModifier pomModifier;
  private final PomFileFinder pomFileFinder;

  MavenProvider(
      final PomModifier pomModifier, final PomFileFinder pomFileFinder, final boolean offline) {
    Objects.requireNonNull(pomModifier);
    Objects.requireNonNull(pomFileFinder);
    this.pomModifier = pomModifier;
    this.pomFileFinder = pomFileFinder;
    this.offline = offline;
  }

  MavenProvider(final PomModifier pomModifier) {
    this(pomModifier, new DefaultPomFileFinder(), true);
  }

  public MavenProvider() {
    this(new DefaultPomModifier(), new DefaultPomFileFinder(), true);
  }

  @VisibleForTesting
  static class DefaultPomFileFinder implements PomFileFinder {
    @Override
    public Optional<Path> findForFile(final Path projectDir, final Path file) throws IOException {
      Path parent = file.getParent();
      while (parent != null && !Files.isSameFile(projectDir.getParent(), parent)) {
        Path pomPath = parent.resolve("pom.xml");
        if (Files.exists(pomPath)) {
          return Optional.of(pomPath);
        }
        parent = parent.getParent();
      }
      return Optional.empty();
    }
  }

  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies) {
    try {
      String dependenciesStr =
          dependencies.stream().map(DependencyGAV::toString).collect(Collectors.joining(","));
      LOG.debug(
          "Updating dependencies for {} in {}: {}", file, projectDir, dependenciesStr);
      DependencyUpdateResult dependencyUpdateResult =
          updateDependenciesInternal(projectDir, file, dependencies);
      LOG.debug("Dependency update result: {}", dependencyUpdateResult);
      return dependencyUpdateResult;
    } catch (Exception e) {
      throw new DependencyUpdateException("Failure when updating dependencies", e);
    }
  }

  @NotNull
  private DependencyUpdateResult updateDependenciesInternal(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies)
      throws IOException {
    Optional<Path> maybePomFile = pomFileFinder.findForFile(projectDir, file);

    if (maybePomFile.isEmpty()) {
      LOG.debug("Pom file was empty for {}", file);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    Path pomFile = maybePomFile.get();
    Set<CodeTFChangesetEntry> changesets = new LinkedHashSet<>();

    List<Dependency> mappedDependencies =
        dependencies.stream()
            .map(
                dependencyGAV ->
                    new Dependency(
                        dependencyGAV.group(),
                        dependencyGAV.artifact(),
                        dependencyGAV.version(),
                        null,
                        null,
                        null))
            .toList();

    final List<DependencyGAV> skippedDependencies = new ArrayList<>();
    final List<DependencyGAV> injectedDependencies = new ArrayList<>();
    final Set<Path> erroredFiles = new LinkedHashSet<>();

    AtomicReference<Collection<DependencyGAV>> foundDependenciesMapped =
        new AtomicReference<>(getDependenciesFrom(pomFile, projectDir));
    LOG.debug("Beginning dependency set size: {}", foundDependenciesMapped.get().size());

    mappedDependencies.forEach(
        newDependency -> {
          DependencyGAV newDependencyGAV =
              DependencyGAV.createDefault(
                  newDependency.getGroupId(),
                  newDependency.getArtifactId(),
                  newDependency.getVersion());

          LOG.debug("Looking at injecting new dependency: {}", newDependencyGAV);
          boolean foundIt =
              foundDependenciesMapped.get().stream().anyMatch(newDependencyGAV::equals);

          if (foundIt) {
            LOG.debug("Found it -- skipping");
            skippedDependencies.add(newDependencyGAV);
            return;
          }

          LOG.debug("Need to inject it...");

          ProjectModelFactory projectModelFactory =
              POMScanner.legacyScanFrom(pomFile.toFile(), projectDir.toFile())
                  .withDependency(newDependency)
                  .withSkipIfNewer(true)
                  .withUseProperties(true)
                  .withOffline(this.offline);

          if (this.offline) {
            try {
              projectModelFactory =
                  projectModelFactory.withRepositoryPath(Files.createTempDirectory(null).toFile());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          ProjectModel projectModel = projectModelFactory.build();

          boolean result = POMOperator.modify(projectModel);

          if (result) {
            LOG.debug("Modified the pom -- writing it back");
            Collection<POMDocument> allPomFiles = projectModel.getAllPomFiles();
            LOG.debug("Found " + allPomFiles.size() + " pom files -- " + allPomFiles);
            for (POMDocument aPomFile : allPomFiles) {
              URI uri;
              try {
                uri = aPomFile.getPomPath().toURI();
              } catch (URISyntaxException ex) {
                ex.printStackTrace();
                throw new DependencyUpdateException("Failure parsing URL: " + aPomFile, ex);
              }

              Path path = Path.of(uri);

              if (aPomFile.getDirty()) {
                LOG.debug("POM file {} was dirty", path);
                try {
                  CodeTFChangesetEntry entry = getChanges(projectDir, aPomFile);
                  LOG.debug("Writing pom...");
                  pomModifier.modify(path, aPomFile.getResultPomBytes());
                  LOG.debug("POM written!");
                  injectedDependencies.add(newDependencyGAV);
                  changesets.add(entry);
                } catch (IOException | UncheckedIOException exc) {
                  LOG.error("Failed to write pom", exc);
                  erroredFiles.add(path);
                }
              } else {
                LOG.debug("POM file {} wasn't dirty", path);
              }
            }

            Collection<DependencyGAV> newDependencySet = getDependenciesFrom(pomFile, projectDir);
            LOG.debug("New dependency set size: {}", newDependencySet.size());
            foundDependenciesMapped.set(newDependencySet);
          } else {
            LOG.debug("POM file didn't need modification or it failed?");
          }
        });

    return DependencyUpdateResult.create(
        injectedDependencies, skippedDependencies, changesets, erroredFiles);
  }

  private List<String> getLinesFrom(final POMDocument doc, final byte[] byteArray) {
    return Arrays.asList(
        new String(byteArray, doc.getCharset()).split(Pattern.quote(doc.getEndl())));
  }

  private CodeTFChangesetEntry getChanges(final Path projectDir, final POMDocument pomDocument) {
    List<String> originalPomContents = getLinesFrom(pomDocument, pomDocument.getOriginalPom());
    List<String> finalPomContents = getLinesFrom(pomDocument, pomDocument.getResultPomBytes());

    Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);

    AbstractDelta<String> delta = patch.getDeltas().get(0);
    int position = 1 + delta.getSource().getPosition();

    Path pomDocumentPath;

    try {
      pomDocumentPath = new File(pomDocument.getPomPath().toURI()).toPath();
    } catch (URISyntaxException e) {
      throw new DependencyUpdateException("Failure on URI for " + pomDocument.getPomPath(), e);
    }

    String relativePomPath = projectDir.relativize(pomDocumentPath).toString();

    CodeTFChange change =
        new CodeTFChange(position, Collections.emptyMap(), "", List.of(), null, List.of());

    List<String> patchDiff =
        UnifiedDiffUtils.generateUnifiedDiff(
            relativePomPath, relativePomPath, originalPomContents, patch, 3);

    String diff = String.join(pomDocument.getEndl(), patchDiff);

    return new CodeTFChangesetEntry(relativePomPath, diff, List.of(change));
  }

  @NotNull
  private Collection<DependencyGAV> getDependenciesFrom(Path pomFile, Path projectDir) {
    ProjectModelFactory projectModelFactory =
        POMScanner.legacyScanFrom(pomFile.toFile(), projectDir.toFile())
            .withQueryType(QueryType.SAFE)
            .withOffline(true);

    if (this.offline) {
      try {
        projectModelFactory =
            projectModelFactory.withRepositoryPath(Files.createTempDirectory(null).toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    ProjectModel originalProjectModel = projectModelFactory.build();

    Collection<Dependency> foundDependencies = POMOperator.queryDependency(originalProjectModel);

    return foundDependencies.stream()
        .map(
            dependency ->
                DependencyGAV.createDefault(
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
        .collect(Collectors.toList());
  }

  private static final Logger LOG = LoggerFactory.getLogger(MavenProvider.class);
}
