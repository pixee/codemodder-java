package io.codemodder.plugins.maven;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
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

  private final PomFileDependencyMapper pomFileDependencyMapper;
  private final PomFileUpdater pomFileUpdater;

  public MavenProvider() {
    this(new DefaultPomFileDependencyMapper(), new DefaultPomFileUpdater());
  }

  MavenProvider(
      final PomFileDependencyMapper pomFileDependencyMapper, final PomFileUpdater pomFileUpdater) {
    this.pomFileDependencyMapper = Objects.requireNonNull(pomFileDependencyMapper);
    this.pomFileUpdater = Objects.requireNonNull(pomFileUpdater);
  }

  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir,
      final Set<ChangedFile> changedFiles,
      final List<FileDependency> remainingFileDependencies)
      throws IOException {
    Set<ChangedFile> newChangedFiles = new HashSet<>(changedFiles);
    Set<Path> erroredFiles = new HashSet<>();
    List<FileDependency> dependenciesInjected = new ArrayList<>();

    // build the map of poms we need to update
    Map<Path, List<FileDependency>> pomsToUpdate =
        pomFileDependencyMapper.build(projectDir, remainingFileDependencies);

    // update the poms, keeping track of what succeeds and fails
    Set<Map.Entry<Path, List<FileDependency>>> entries = pomsToUpdate.entrySet();
    for (Map.Entry<Path, List<FileDependency>> entry : entries) {
      Path pomPath = entry.getKey();
      List<FileDependency> dependencies = entry.getValue();

      // we have to introduce some complexity here to handle the case where one of our codemods has
      // already updated
      // this pom. in that case, we need to apply our changes on top of the existing changes. to do
      // that, we have to
      // follow this process:
      // 1. backup the existing pom, which is still actually unchanged in the filesystem
      // 2. apply the existing changes to the pom on disk so when we "visit" it we are acting on the
      // updated version
      // 3. if we changed it again, update the changedFile record to reflect the changes before +
      // new changes
      // 4. restore the backup pom
      Optional<Path> backupFile = Optional.empty();
      Optional<ChangedFile> existingChangeRecord = Optional.empty();
      if (fileAlreadyHasChanges(pomPath, changedFiles)) {
        Path pomBackup = Files.createTempFile("backup", ".pom");
        Files.copy(pomPath, pomBackup, StandardCopyOption.REPLACE_EXISTING);
        backupFile = Optional.of(pomBackup);
        ChangedFile existingChangeForPom =
            changedFiles.stream()
                .filter(changedFile -> isSameFile(Path.of(changedFile.originalFilePath()), pomPath))
                .findFirst()
                .get();
        existingChangeRecord = Optional.of(existingChangeForPom);
        Files.copy(
            Path.of(existingChangeForPom.modifiedFile()),
            pomPath,
            StandardCopyOption.REPLACE_EXISTING);
      }
      try {
        Optional<ChangedFile> changedPom = pomFileUpdater.updatePom(pomPath, dependencies);
        if (changedPom.isPresent()) {
          dependenciesInjected.addAll(dependencies);
          // remove the backup from the change set, if we have one
          existingChangeRecord.ifPresent(newChangedFiles::remove);
          newChangedFiles.add(changedPom.get());
        }
      } catch (IOException e) {
        erroredFiles.add(pomPath);
      }
      if (backupFile.isPresent()) {
        Files.copy(backupFile.get(), pomPath, StandardCopyOption.REPLACE_EXISTING);
        Files.delete(backupFile.get());
      }
    }

    return DependencyUpdateResult.create(dependenciesInjected, newChangedFiles, erroredFiles);
  }

  private boolean fileAlreadyHasChanges(final Path path, final Set<ChangedFile> changedFiles) {
    return changedFiles.stream()
        .anyMatch(changedFile -> isSameFile(Path.of(changedFile.originalFilePath()), path));
  }

  @VisibleForTesting
  static class DefaultPomFileDependencyMapper implements PomFileDependencyMapper {
    @Override
    public Map<Path, List<FileDependency>> build(
        final Path projectDir, final List<FileDependency> fileDependencies) throws IOException {
      Map<Path, List<FileDependency>> pomsToUpdate = new HashMap<>();
      for (FileDependency fileDependency : fileDependencies) {
        Path filePath = fileDependency.file();
        Optional<Path> pomPath = findParentPom(projectDir, filePath);
        pomPath.ifPresent(
            path -> {
              if (pomsToUpdate.containsKey(path)) {
                pomsToUpdate.get(path).add(fileDependency);
              } else {
                pomsToUpdate.put(path, new ArrayList<>(Collections.singletonList(fileDependency)));
              }
            });
      }

      return pomsToUpdate;
    }

    private Optional<Path> findParentPom(final Path projectDir, final Path filePath)
        throws IOException {
      Path parent = filePath.getParent();
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
    public Optional<ChangedFile> updatePom(
        final Path pomPath, final List<FileDependency> dependencies) throws IOException {
      List<io.openpixee.maven.operator.Dependency> mappedDependencies =
          dependencies.stream()
              .map(FileDependency::dependencies)
              .flatMap(Collection::stream)
              .distinct()
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

      return Optional.of(
          ChangedFile.createDefault(
              pomPath.toAbsolutePath().toString(),
              newPomFile.toAbsolutePath().toString(),
              Weave.from(position, pomInjectionRuleId)));
    }
  }

  private boolean isSameFile(Path p1, Path p2) {
    try {
      return Files.isSameFile(p1, p2);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final String pomInjectionRuleId = "pixee:java/maven-pom-injection";
}
