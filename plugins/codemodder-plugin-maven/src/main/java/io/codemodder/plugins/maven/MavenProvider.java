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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

/** Provides Maven dependency management functions to codemods. */
public final class MavenProvider implements ProjectProvider {
  public interface POMModifier {
    boolean modify(Path path, byte[] contents) throws IOException;
  }

  @VisibleForTesting
  public static class DefaultPOMModifier implements POMModifier {
    @Override
    public boolean modify(Path path, byte[] contents) throws IOException {
      Files.write(path, contents);

      return false;
    }
  }

  private final POMModifier pomModifier;

  public MavenProvider(POMModifier pomModifier) {
    this.pomModifier = pomModifier;
  }

  public MavenProvider() {
    this(new DefaultPOMModifier());
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
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies)
      throws IOException {
    Optional<Path> maybePomFile = new DefaultPomFileFinder().findForFile(projectDir, file);

    if (maybePomFile.isEmpty()) {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    Path pomFile = maybePomFile.get();

    Set<CodeTFChangesetEntry> changesets = new LinkedHashSet<>();
    Set<Path> failedFiles = new LinkedHashSet<>();

    List<io.github.pixee.maven.operator.Dependency> mappedDependencies =
        dependencies.stream()
            .map(
                dependencyGAV ->
                    new io.github.pixee.maven.operator.Dependency(
                        dependencyGAV.group(),
                        dependencyGAV.artifact(),
                        dependencyGAV.version(),
                        null,
                        null,
                        null))
            .toList();

    final List<DependencyGAV> skippedDependencies = new ArrayList<>();
    final List<DependencyGAV> failedDependencies = new ArrayList<>();

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
                // TODO Log

                throw new RuntimeException(ex);
              }

              Path path = Path.of(uri);

              if (aPomFile.getDirty()) {
                try {
                  CodeTFChangesetEntry entry = getChanges(projectDir, aPomFile);

                  changesets.add(entry);

                  pomModifier.modify(path, aPomFile.getResultPomBytes());
                } catch (Exception e) {
                  // TODO Log
                  failedFiles.add(path);
                }
              }
            }

            foundDependenciesMapped.set(getDependenciesFrom(pomFile));
          }
        });

    return DependencyUpdateResult.create(dependencies, skippedDependencies, changesets, Set.of());
  }

  private boolean validPomFileName(Path path) {
    Path fileName = path.getFileName();

    return fileName.startsWith("pom") && fileName.endsWith(".xml");
  }

  private CodeTFChangesetEntry getChanges(Path projectDir, POMDocument pomDocument)
      throws URISyntaxException {
    List<String> originalPomContents =
        Arrays.asList(
            new String(pomDocument.getResultPomBytes(), pomDocument.getCharset())
                .split(Pattern.quote(pomDocument.getEndl())));
    List<String> finalPomContents =
        Arrays.asList(
            new String(pomDocument.getOriginalPom(), pomDocument.getCharset())
                .split(Pattern.quote(pomDocument.getEndl())));

    Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);

    AbstractDelta<String> delta = patch.getDeltas().get(0);
    int position = 1 + delta.getSource().getPosition();

    Path pomDocumentPath = new File(pomDocument.getPomPath().toURI()).toPath();

    String relativePomPath = projectDir.relativize(pomDocumentPath).toString();

    CodeTFChange change =
        new CodeTFChange(position, Collections.emptyMap(), "", List.of(), null, List.of());

    List<String> patchDiff =
        UnifiedDiffUtils.generateUnifiedDiff(
            relativePomPath, relativePomPath, originalPomContents, patch, 3);

    String diff = String.join(pomDocument.getEndl(), patchDiff);

    CodeTFChangesetEntry entry = new CodeTFChangesetEntry(relativePomPath, diff, List.of(change));

    return entry;
  }

  @NotNull
  private static Collection<DependencyGAV> getDependenciesFrom(Path pomFile) {
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
