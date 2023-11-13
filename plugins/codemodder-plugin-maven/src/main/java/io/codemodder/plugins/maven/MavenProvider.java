package io.codemodder.plugins.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.plugins.maven.operator.Dependency;
import io.codemodder.plugins.maven.operator.POMDocument;
import io.codemodder.plugins.maven.operator.POMOperator;
import io.codemodder.plugins.maven.operator.POMScanner;
import io.codemodder.plugins.maven.operator.ProjectModel;
import io.codemodder.plugins.maven.operator.ProjectModelFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Maven dependency management functions to codemods.
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

  private final DependencyDescriptor dependencyDescriptor;

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

  private final PomModifier pomModifier;
  private final PomFileFinder pomFileFinder;
  private final ArtifactInjectionPositionFinder positionFinder;

  MavenProvider(
      final PomModifier pomModifier,
      final PomFileFinder pomFileFinder,
      final DependencyDescriptor dependencyDescriptor,
      final ArtifactInjectionPositionFinder positionFinder) {
    Objects.requireNonNull(pomModifier);
    Objects.requireNonNull(pomFileFinder);
    this.pomModifier = pomModifier;
    this.pomFileFinder = pomFileFinder;
    this.dependencyDescriptor = Objects.requireNonNull(dependencyDescriptor);
    this.positionFinder = Objects.requireNonNull(positionFinder);
  }

  MavenProvider(
      final PomModifier pomModifier,
      final PomFileFinder pomFileFinder,
      final DependencyDescriptor dependencyDescriptor) {
    this(
        pomModifier,
        pomFileFinder,
        dependencyDescriptor,
        new DefaultArtifactInjectionPositionFinder());
  }

  MavenProvider(final PomModifier pomModifier) {
    this(
        pomModifier,
        new DefaultPomFileFinder(),
        DependencyDescriptor.createMarkdownDescriptor(),
        new DefaultArtifactInjectionPositionFinder());
  }

  public MavenProvider() {
    this(
        new DefaultPomModifier(),
        new DefaultPomFileFinder(),
        DependencyDescriptor.createMarkdownDescriptor(),
        new DefaultArtifactInjectionPositionFinder());
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
      LOG.trace("Updating dependencies for {} in {}: {}", file, projectDir, dependenciesStr);
      DependencyUpdateResult dependencyUpdateResult =
          updateDependenciesInternal(projectDir, file, dependencies);
      LOG.trace("Dependency update result: {}", dependencyUpdateResult);
      return dependencyUpdateResult;
    } catch (Exception e) {
      throw new DependencyUpdateException("Failure when updating dependencies", e);
    }
  }

  @NotNull
  private DependencyUpdateResult updateDependenciesInternal(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies)
      throws IOException, DocumentException, URISyntaxException, XMLStreamException {
    Optional<Path> maybePomFile = pomFileFinder.findForFile(projectDir, file);

    if (maybePomFile.isEmpty()) {
      LOG.trace("Pom file was empty for {}", file);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    Path pomFile = maybePomFile.get();
    List<CodeTFChangesetEntry> changesets = new ArrayList<>();

    final List<DependencyGAV> skippedDependencies = new ArrayList<>();
    final List<DependencyGAV> injectedDependencies = new ArrayList<>();
    final Set<Path> erroredFiles = new LinkedHashSet<>();

    AtomicReference<Collection<DependencyGAV>> foundDependenciesMapped =
        new AtomicReference<>(getDependenciesFrom(pomFile, projectDir));
    LOG.trace("Beginning dependency set size: {}", foundDependenciesMapped.get().size());

    dependencies.forEach(
        newDependencyGAV -> {
          LOG.trace("Looking at injecting new dependency: {}", newDependencyGAV);
          boolean foundIt =
              foundDependenciesMapped.get().stream().anyMatch(newDependencyGAV::equals);

          if (foundIt) {
            LOG.trace("Found it -- skipping");
            skippedDependencies.add(newDependencyGAV);
            return;
          }

          LOG.trace("Need to inject it...");
          Dependency newDependency =
              new Dependency(
                  newDependencyGAV.group(),
                  newDependencyGAV.artifact(),
                  newDependencyGAV.version(),
                  null,
                  null,
                  null);
          ProjectModelFactory projectModelFactory = null;
          try {
            projectModelFactory =
                POMScanner.legacyScanFrom(pomFile.toFile(), projectDir.toFile())
                    .withDependency(newDependency)
                    .withSkipIfNewer(true)
                    .withUseProperties(true);
          } catch (DocumentException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          }

          try {
            projectModelFactory =
                projectModelFactory.withRepositoryPath(Files.createTempDirectory(null).toFile());
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

          ProjectModel projectModel = projectModelFactory.build();

          boolean result = false;
          try {
            result = POMOperator.modify(projectModel);
          } catch (URISyntaxException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          } catch (XMLStreamException e) {
            throw new RuntimeException(e);
          }

          if (result) {
            LOG.trace("Modified the pom -- writing it back");
            Collection<POMDocument> allPomFiles = projectModel.allPomFiles();
            LOG.trace("Found " + allPomFiles.size() + " pom files -- " + allPomFiles);
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
                LOG.trace("POM file {} was dirty", path);
                try {
                  CodeTFChangesetEntry entry = getChanges(projectDir, aPomFile, newDependencyGAV);
                  pomModifier.modify(path, aPomFile.getResultPomBytes());
                  LOG.trace("POM written!");
                  injectedDependencies.add(newDependencyGAV);
                  changesets.add(entry);
                } catch (IOException | UncheckedIOException exc) {
                  LOG.error("Failed to write pom", exc);
                  erroredFiles.add(path);
                }
              } else {
                LOG.trace("POM file {} wasn't dirty", path);
              }
            }

            Collection<DependencyGAV> newDependencySet = null;
            try {
              newDependencySet = getDependenciesFrom(pomFile, projectDir);
            } catch (DocumentException e) {
              throw new RuntimeException(e);
            } catch (IOException e) {
              throw new RuntimeException(e);
            } catch (URISyntaxException e) {
              throw new RuntimeException(e);
            } catch (XMLStreamException e) {
              throw new RuntimeException(e);
            }
            LOG.trace("New dependency set size: {}", newDependencySet.size());
            foundDependenciesMapped.set(newDependencySet);
          } else {
            LOG.trace("POM file didn't need modification or it failed?");
          }
        });

    return DependencyUpdateResult.create(
        injectedDependencies, skippedDependencies, changesets, erroredFiles);
  }

  private List<String> getLinesFrom(final POMDocument doc, final byte[] byteArray) {
    return Arrays.asList(
        new String(byteArray, doc.getCharset()).split(Pattern.quote(doc.getEndl())));
  }

  private CodeTFChangesetEntry getChanges(
      final Path projectDir, final POMDocument pomDocument, final DependencyGAV newDependency) {
    List<String> originalPomContents = getLinesFrom(pomDocument, pomDocument.getOriginalPom());
    List<String> finalPomContents = getLinesFrom(pomDocument, pomDocument.getResultPomBytes());

    Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);

    List<AbstractDelta<String>> deltas = patch.getDeltas();
    int position = positionFinder.find(deltas, newDependency.artifact());

    Path pomDocumentPath;

    try {
      pomDocumentPath = new File(pomDocument.getPomPath().toURI()).toPath();
    } catch (URISyntaxException e) {
      throw new DependencyUpdateException("Failure on URI for " + pomDocument.getPomPath(), e);
    }

    String relativePomPath = projectDir.relativize(pomDocumentPath).toString();

    String description = dependencyDescriptor.create(newDependency);
    final Map<String, String> properties;
    if (description != null && !description.isBlank()) {
      /*
       * Tell downstream consumers that this is a change based on surrounding context.
       */
      properties = Map.of("contextual_description", "true");
    } else {
      properties = Collections.emptyMap();
    }
    CodeTFChange change = new CodeTFChange(position, properties, description, List.of(), List.of());

    List<String> patchDiff =
        UnifiedDiffUtils.generateUnifiedDiff(
            relativePomPath, relativePomPath, originalPomContents, patch, 3);

    String diff = String.join(pomDocument.getEndl(), patchDiff);

    return new CodeTFChangesetEntry(relativePomPath, diff, List.of(change));
  }

  @NotNull
  private Collection<DependencyGAV> getDependenciesFrom(final Path pomFile, final Path projectDir)
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModelFactory projectModelFactory =
        POMScanner.legacyScanFrom(pomFile.toFile(), projectDir.toFile()).withSafeQueryType();

    try {
      projectModelFactory =
          projectModelFactory.withRepositoryPath(Files.createTempDirectory(null).toFile());
    } catch (IOException e) {
      throw new RuntimeException(e);
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
