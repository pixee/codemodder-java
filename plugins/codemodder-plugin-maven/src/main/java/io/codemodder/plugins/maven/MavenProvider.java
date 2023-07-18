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

/** Provides Maven dependency management functions to codemods. */
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

  private final PomModifier pomModifier;
  private final PomFileFinder pomFileFinder;

  MavenProvider(final PomModifier pomModifier, final PomFileFinder pomFileFinder) {
    Objects.requireNonNull(pomModifier);
    Objects.requireNonNull(pomFileFinder);
    this.pomModifier = pomModifier;
    this.pomFileFinder = pomFileFinder;
  }

  MavenProvider(final PomModifier pomModifier) {
    this(pomModifier, new DefaultPomFileFinder());
  }

  public MavenProvider() {
    this(new DefaultPomModifier(), new DefaultPomFileFinder());
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
      return updateDependenciesInternal(projectDir, file, dependencies);
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
        new AtomicReference<>(getDependenciesFrom(pomFile));

    mappedDependencies.forEach(
        newDependency -> {
          DependencyGAV newDependencyGAV =
              DependencyGAV.createDefault(
                  newDependency.getGroupId(),
                  newDependency.getArtifactId(),
                  newDependency.getVersion());

          boolean foundIt =
              foundDependenciesMapped.get().stream().anyMatch(newDependencyGAV::equals);

          if (foundIt) {
            skippedDependencies.add(newDependencyGAV);

            return;
          }

          ProjectModel projectModel =
              POMScanner.scanFrom(pomFile.toFile(), projectDir.toFile())
                  .withDependency(newDependency)
                  .withSkipIfNewer(true)
                  .withUseProperties(true)
                  .build();

          boolean result = POMOperator.modify(projectModel);

          if (result) {
            for (POMDocument aPomFile : projectModel.getAllPomFiles()) {
              URI uri;

              try {
                uri = aPomFile.getPomPath().toURI();
              } catch (URISyntaxException ex) {
                throw new DependencyUpdateException("Failure parsing URL: " + aPomFile, ex);
              }

              Path path = Path.of(uri);

              if (aPomFile.getDirty()) {
                try {
                  CodeTFChangesetEntry entry = getChanges(projectDir, aPomFile);
                  pomModifier.modify(path, aPomFile.getResultPomBytes());
                  injectedDependencies.add(newDependencyGAV);
                  changesets.add(entry);
                } catch (IOException | UncheckedIOException exc) {
                  erroredFiles.add(path);
                }
              }
            }

            foundDependenciesMapped.set(getDependenciesFrom(pomFile));
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
  private static Collection<DependencyGAV> getDependenciesFrom(final Path pomFile) {
    ProjectModel originalProjectModel =
        ProjectModelFactory.load(pomFile.toFile()).withQueryType(QueryType.SAFE).build();

    Collection<Dependency> foundDependencies = POMOperator.queryDependency(originalProjectModel);

    return foundDependencies.stream()
        .map(
            dependency ->
                DependencyGAV.createDefault(
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
        .collect(Collectors.toList());
  }
}
