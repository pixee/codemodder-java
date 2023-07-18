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
import io.github.pixee.maven.operator.POMOperator;
import io.github.pixee.maven.operator.ProjectModel;
import io.github.pixee.maven.operator.ProjectModelFactory;
import io.github.pixee.maven.operator.QueryType;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.mozilla.universalchardet.UniversalDetector;

/** Provides Maven dependency management functions to codemods. */
public final class MavenProvider implements ProjectProvider {

  private final PomFileFinder pomFileFinder;
  private final PomFileUpdater pomFileUpdater;

  @SuppressWarnings("unused")
  public MavenProvider() {
    this(new DefaultPomFileFinder(), new DefaultPomFileUpdater());
  }

  MavenProvider(final PomFileFinder pomFileFinder, final PomFileUpdater pomFileUpdater) {
    this.pomFileFinder = Objects.requireNonNull(pomFileFinder);
    this.pomFileUpdater = Objects.requireNonNull(pomFileUpdater);
  }

  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies)
      throws IOException {

    // find the map pom.xml path of the pom we need to update
    Optional<Path> targetPomRef = pomFileFinder.findForFile(projectDir, file);
    if (targetPomRef.isEmpty()) {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    Path targetPom = targetPomRef.get();
    try {
      PomUpdateResult pomUpdateResult =
          pomFileUpdater.updatePom(projectDir, targetPom, dependencies);
      Optional<CodeTFChangesetEntry> entryRef = pomUpdateResult.getEntry();
      List<DependencyGAV> skippedDependencies = pomUpdateResult.getSkippedDependencies();
      if (entryRef.isPresent()) {
        return DependencyUpdateResult.create(
            dependencies, skippedDependencies, Set.of(entryRef.get()), Set.of());
      }
      return DependencyUpdateResult.create(List.of(), skippedDependencies, Set.of(), Set.of());
    } catch (IOException e) {
      return DependencyUpdateResult.create(List.of(), List.of(), Set.of(), Set.of(targetPom));
    }
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

  @VisibleForTesting
  static class DefaultPomFileUpdater implements PomFileUpdater {
    @Override
    public PomUpdateResult updatePom(
        final Path projectDir, final Path pomPath, final List<DependencyGAV> dependencies)
        throws IOException {
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
              .collect(Collectors.toList());

      String charsetDetected = UniversalDetector.detectCharset(pomPath);
      if (charsetDetected == null) {
        charsetDetected = "UTF-8";
      }
      Charset charset = Charset.forName(charsetDetected);
      var originalPomContents = Files.readAllLines(pomPath, charset);

      final Path newPomFile = Files.createTempFile("pom", ".xml");
      Files.copy(pomPath, newPomFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      final List<DependencyGAV> skippedDependencies = new ArrayList<>();
      final List<DependencyGAV> failedDependencies = new ArrayList<>();

      AtomicReference<Collection<DependencyGAV>> foundDependenciesMapped =
          new AtomicReference<>(getDependenciesFrom(pomPath));

      final AtomicReference<String> detectedEndline = new AtomicReference<>(null);
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
                ProjectModelFactory.load(newPomFile.toFile())
                    .withDependency(newDependency)
                    .withSkipIfNewer(true)
                    .withUseProperties(true)
                    .build();
            detectedEndline.set(projectModel.getEndl());

            boolean result = POMOperator.modify(projectModel);

            if (result) {
              try {
                Files.write(newPomFile, projectModel.getResultPomBytes());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              // Dependencies got dirty and there's the slight risk of introducing
              // transitive ones we didn't expect - let's rebuild it
              foundDependenciesMapped.set(getDependenciesFrom(newPomFile));
            } else {
              failedDependencies.add(newDependencyGAV);
            }
          });

      var finalPomContents = Files.readAllLines(newPomFile, charset);
      if (finalPomContents.equals(originalPomContents)) {
        return new PomUpdateResult(Optional.empty(), skippedDependencies);
      }

      Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);
      AbstractDelta<String> delta = patch.getDeltas().get(0);
      int position = 1 + delta.getSource().getPosition();

      String relativePomPath = projectDir.relativize(pomPath).toString();
      CodeTFChange change =
          new CodeTFChange(position, Collections.emptyMap(), "", List.of(), null, List.of());
      List<String> patchDiff =
          UnifiedDiffUtils.generateUnifiedDiff(
              relativePomPath, relativePomPath, originalPomContents, patch, 3);

      String endline = detectedEndline.get();
      if (endline == null || endline.isEmpty()) {
        endline = "\n";
      }
      String diff = String.join(endline, patchDiff);
      CodeTFChangesetEntry entry = new CodeTFChangesetEntry(relativePomPath, diff, List.of(change));

      // overwrite existing pom
      Files.copy(newPomFile, pomPath, StandardCopyOption.REPLACE_EXISTING);

      return new PomUpdateResult(Optional.of(entry), skippedDependencies);
    }

    @NotNull
    private static Collection<DependencyGAV> getDependenciesFrom(Path newPomFile) {
      ProjectModel originalProjectModel =
          ProjectModelFactory.load(newPomFile.toFile()).withQueryType(QueryType.SAFE).build();

      Collection<Dependency> foundDependencies = POMOperator.queryDependency(originalProjectModel);

      return foundDependencies.stream()
          .map(
              dependency ->
                  DependencyGAV.createDefault(
                      dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
          .collect(Collectors.toList());
    }
  }
}
