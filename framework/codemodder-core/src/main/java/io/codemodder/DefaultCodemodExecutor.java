package io.codemodder;

import static java.util.Collections.emptyMap;

import com.github.javaparser.JavaParser;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFPackageAction;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.javaparser.JavaParserCodemodRunner;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

final class DefaultCodemodExecutor implements CodemodExecutor {

  private final CodemodIdPair codemod;
  private final List<ProjectProvider> projectProviders;
  private final JavaParser javaParser;
  private final Path projectDir;
  private final IncludesExcludes includesExcludes;
  private final EncodingDetector encodingDetector;

  DefaultCodemodExecutor(
      Path projectDir,
      IncludesExcludes includesExcludes,
      CodemodIdPair codemod,
      List<ProjectProvider> projectProviders,
      JavaParser javaParser,
      EncodingDetector encodingDetector) {
    this.projectDir = projectDir;
    this.includesExcludes = includesExcludes;
    this.codemod = codemod;
    this.projectProviders = projectProviders;
    this.javaParser = javaParser;
    this.encodingDetector = encodingDetector;
  }

  @Override
  public CodeTFResult execute(final List<Path> filePaths) {
    List<CodeTFChangesetEntry> changeset = new ArrayList<>();
    Set<Path> unscannableFiles = new HashSet<>();
    DefaultCodeDirectory codeDirectory = new DefaultCodeDirectory(projectDir);
    Changer changer = codemod.getChanger();

    /*
     *  Create the right CodemodRunner based on the type of Changer and filer the files to be processed by them.
     */
    CodemodRunner codemodRunner;
    List<Path> codemodTargetFiles;
    if (changer instanceof JavaParserChanger) {
      codemodTargetFiles = filePaths.stream().filter(this::isJavaFile).collect(Collectors.toList());
      codemodRunner =
          new JavaParserCodemodRunner(javaParser, (JavaParserChanger) changer, encodingDetector);
    } else if (changer instanceof RawFileChanger) {
      codemodTargetFiles =
          filePaths.stream().filter(this::isNotJavaFile).collect(Collectors.toList());
      codemodRunner = new RawFileCodemodRunner((RawFileChanger) changer, encodingDetector);
    } else {
      throw new UnsupportedOperationException(
          "unsupported changer type: " + changer.getClass().getName());
    }

    for (Path filePath : codemodTargetFiles) {
      // create the context necessary for the codemod to run
      LineIncludesExcludes lineIncludesExcludes =
          includesExcludes.getIncludesExcludesForFile(filePath.toFile());
      CodemodInvocationContext context =
          new DefaultCodemodInvocationContext(
              codeDirectory, filePath, codemod.getId(), lineIncludesExcludes);
      try {
        // run the codemod on the file
        List<CodemodChange> codemodChanges = codemodRunner.run(context);

        if (!codemodChanges.isEmpty()) {
          // update the dependencies in the manifest file if needed
          List<DependencyGAV> dependencies =
              codemodChanges.stream()
                  .map(CodemodChange::getDependenciesNeeded)
                  .flatMap(Collection::stream)
                  .distinct()
                  .collect(Collectors.toList());
          List<CodeTFPackageAction> pkgActions;
          List<CodeTFChangesetEntry> dependencyChangesetEntries = Collections.emptyList();
          if (!dependencies.isEmpty()) {
            CodemodPackageUpdateResult packageAddResult = addPackages(filePath, dependencies);
            unscannableFiles.addAll(packageAddResult.filesFailedToChange());
            pkgActions = packageAddResult.packageActions();
            dependencyChangesetEntries = packageAddResult.manifestChanges();
          } else {
            pkgActions = Collections.emptyList();
          }

          // record the change for the file
          List<CodeTFChange> changes =
              codemodChanges.stream()
                  .map(
                      change ->
                          translateCodemodChangetoCodeTFChange(
                              changer, filePath, change, pkgActions))
                  .collect(Collectors.toList());

          // make sure we add the file's entry first, then the dependency entries, so the causality
          // is clear
          changeset.add(
              new CodeTFChangesetEntry(getRelativePath(projectDir, filePath), "", changes));
          changeset.addAll(dependencyChangesetEntries);
        }
      } catch (Exception e) {
        unscannableFiles.add(filePath);
      }
    }
    return new CodeTFResult(
        codemod.getId(),
        changer.getSummary(),
        changer.getDescription(),
        unscannableFiles.stream()
            .map(file -> getRelativePath(projectDir, file))
            .collect(Collectors.toSet()),
        changer.getReferences(),
        emptyMap(),
        changeset);
  }

  @NotNull
  private static CodeTFChange translateCodemodChangetoCodeTFChange(
      final Changer changer,
      final Path filePath,
      final CodemodChange change,
      final List<CodeTFPackageAction> pkgActions) {
    return new CodeTFChange(
        change.lineNumber(),
        emptyMap(),
        changer.getIndividualChangeDescription(filePath, change),
        pkgActions);
  }

  /**
   * After updating the files, this method asks the project providers to apply any corrective
   * changers to the project as a whole regarding the dependencies. This is useful for things like
   * adding a dependency to the project's pom.xml file. Eventually we want support other operations
   * besides "add" (like, obviously, "remove")
   */
  private CodemodPackageUpdateResult addPackages(
      final Path file, final List<DependencyGAV> dependencies) throws IOException {
    List<CodeTFPackageAction> pkgActions = new ArrayList<>();
    Set<Path> unscannableFiles = new HashSet<>();
    List<CodeTFChangesetEntry> pkgChanges = new ArrayList<>();
    for (ProjectProvider projectProvider : projectProviders) {
      DependencyUpdateResult result =
          projectProvider.updateDependencies(projectDir, file, dependencies);
      unscannableFiles.addAll(
          result.erroredFiles().stream().map(Path::toAbsolutePath).collect(Collectors.toList()));
      dependencies.removeAll(result.injectedPackages());
      pkgChanges.addAll(result.packageChanges());
    }
    return CodemodPackageUpdateResult.from(pkgActions, pkgChanges, unscannableFiles);
  }

  /** Return the relative path name (e.g., src/test/foo) of a file within the project dir. */
  private String getRelativePath(final Path projectDir, final Path filePath) {
    return projectDir.relativize(filePath).toString();
  }

  private boolean isJavaFile(final Path filePath) {
    return filePath.getFileName().toString().toLowerCase().endsWith(".java");
  }

  private boolean isNotJavaFile(final Path filePath) {
    return !isJavaFile(filePath);
  }
}
