package io.codemodder.plugins.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.openpixee.maven.operator.POMOperator;
import io.openpixee.maven.operator.ProjectModel;
import io.openpixee.maven.operator.ProjectModelFactory;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;

/** Provides Maven dependency management functions to codemods. */
public final class MavenProvider implements ProjectProvider {

  private final PomFileFinder pomFileFinder;
  private final PomFileUpdater pomFileUpdater;

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
      Optional<CodeTFChangesetEntry> entryRef =
          pomFileUpdater.updatePom(projectDir, targetPom, dependencies);
      if (entryRef.isPresent()) {
        return DependencyUpdateResult.create(dependencies, Set.of(entryRef.get()), Set.of());
      }
    } catch (IOException e) {
      return DependencyUpdateResult.create(List.of(), Set.of(), Set.of(targetPom));
    }

    return DependencyUpdateResult.EMPTY_UPDATE;
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
    public Optional<CodeTFChangesetEntry> updatePom(
        final Path projectDir, final Path pomPath, final List<DependencyGAV> dependencies)
        throws IOException {
      List<io.openpixee.maven.operator.Dependency> mappedDependencies =
          dependencies.stream()
              .map(
                  dependencyGAV ->
                      new io.openpixee.maven.operator.Dependency(
                          dependencyGAV.group(),
                          dependencyGAV.artifact(),
                          dependencyGAV.version(),
                          null,
                          null,
                          null))
              .collect(Collectors.toList());

      var originalPomContents = Files.readAllLines(pomPath, Charset.defaultCharset());

      final Path newPomFile = Files.createTempFile("pom", ".xml");
      Files.copy(pomPath, newPomFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

      mappedDependencies.forEach(
          newDependency -> {
            ProjectModel projectModel =
                ProjectModelFactory.load(newPomFile.toFile())
                    .withDependency(newDependency)
                    .withOverrideIfAlreadyExists(true)
                    .withSkipIfNewer(true)
                    .withUseProperties(true)
                    .build();

            boolean result = POMOperator.modify(projectModel);

            if (result) {
              try {
                Files.write(newPomFile, projectModel.getResultPomBytes());
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }
          });

      var finalPomContents = Files.readAllLines(newPomFile, Charset.defaultCharset());
      if (finalPomContents.equals(originalPomContents)) {
        return Optional.empty();
      }

      Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);
      AbstractDelta<String> delta = patch.getDeltas().get(0);
      int position = 1 + delta.getSource().getPosition();

      CodeTFChange change = new CodeTFChange(position, Collections.emptyMap(), "", List.of());
      String diff = DiffUtils.diff(originalPomContents, finalPomContents).toString();
      CodeTFChangesetEntry entry =
          new CodeTFChangesetEntry(
              projectDir.relativize(pomPath).toString(), diff, List.of(change));

      // overwrite existing pom
      Files.copy(newPomFile, pomPath, StandardCopyOption.REPLACE_EXISTING);

      return Optional.of(entry);
    }
  }
}
