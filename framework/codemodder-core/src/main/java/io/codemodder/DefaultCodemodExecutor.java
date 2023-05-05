package io.codemodder;

import static java.util.Collections.emptyMap;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFPackageAction;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.javaparser.JavaParserCodemodRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

final class DefaultCodemodExecutor implements CodemodExecutor {

  private final CodemodIdPair codemod;
  private final List<ProjectProvider> projectProviders;
  private final CachingJavaParser cachingJavaParser;
  private final Path projectDir;
  private final IncludesExcludes includesExcludes;
  private final EncodingDetector encodingDetector;

  DefaultCodemodExecutor(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final CachingJavaParser cachingJavaParser,
      final EncodingDetector encodingDetector) {
    this.projectDir = Objects.requireNonNull(projectDir);
    this.includesExcludes = Objects.requireNonNull(includesExcludes);
    this.codemod = Objects.requireNonNull(codemod);
    this.projectProviders = Objects.requireNonNull(projectProviders);
    this.cachingJavaParser = Objects.requireNonNull(cachingJavaParser);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
  }

  @Override
  public CodeTFResult execute(final List<Path> filePaths) {
    List<CodeTFChangesetEntry> changeset = new ArrayList<>();
    Set<Path> unscannableFiles = new HashSet<>();
    DefaultCodeDirectory codeDirectory = new DefaultCodeDirectory(projectDir);
    Changer changer = codemod.getChanger();

    /*
     *  Create the right CodemodRunner based on the type of Changer.
     */
    CodemodRunner codemodRunner;
    if (changer instanceof JavaParserChanger) {
      codemodRunner =
          new JavaParserCodemodRunner(
              cachingJavaParser, (JavaParserChanger) changer, encodingDetector);
    } else if (changer instanceof RawFileChanger) {
      codemodRunner = new RawFileCodemodRunner((RawFileChanger) changer, encodingDetector);
    } else {
      throw new UnsupportedOperationException(
          "unsupported changer type: " + changer.getClass().getName());
    }

    /*
     * Filter the files to those that the CodemodRunner supports.
     */
    List<Path> codemodTargetFiles =
        filePaths.stream().filter(codemodRunner::supports).collect(Collectors.toList());

    for (Path filePath : codemodTargetFiles) {

      // create the context necessary for the codemod to run
      LineIncludesExcludes lineIncludesExcludes =
          includesExcludes.getIncludesExcludesForFile(filePath.toFile());
      CodemodInvocationContext context =
          new DefaultCodemodInvocationContext(
              codeDirectory, filePath, codemod.getId(), lineIncludesExcludes);
      try {
        // capture the "before" for the diff, if needed
        List<String> beforeFile = Files.readAllLines(filePath);

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
          List<String> afterFile = Files.readAllLines(filePath);
          List<String> patchDiff =
              UnifiedDiffUtils.generateUnifiedDiff(
                  filePath.getFileName().toString(),
                  filePath.getFileName().toString(),
                  beforeFile,
                  DiffUtils.diff(beforeFile, afterFile),
                  3);

          String diff = String.join("\n", patchDiff);
          changeset.add(
              new CodeTFChangesetEntry(getRelativePath(projectDir, filePath), diff, changes));
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
    List<DependencyGAV> skippedDependencies = new ArrayList<>();
    List<CodeTFChangesetEntry> pkgChanges = new ArrayList<>();
    for (ProjectProvider projectProvider : projectProviders) {
      DependencyUpdateResult result =
          projectProvider.updateDependencies(projectDir, file, dependencies);
      unscannableFiles.addAll(
          result.erroredFiles().stream().map(Path::toAbsolutePath).collect(Collectors.toList()));
      pkgChanges.addAll(result.packageChanges());
      for (DependencyGAV dependency : result.injectedPackages()) {
        String packageUrl = toPackageUrl(dependency);
        pkgActions.add(
            new CodeTFPackageAction(
                CodeTFPackageAction.CodeTFPackageActionType.ADD,
                CodeTFPackageAction.CodeTFPackageActionResult.COMPLETED,
                packageUrl));
      }
      for (DependencyGAV dependency : result.skippedPackages()) {
        String packageUrl = toPackageUrl(dependency);
        skippedDependencies.add(dependency);
        pkgActions.add(
            new CodeTFPackageAction(
                CodeTFPackageAction.CodeTFPackageActionType.ADD,
                CodeTFPackageAction.CodeTFPackageActionResult.SKIPPED,
                packageUrl));
      }
      dependencies.removeAll(new HashSet<>(result.injectedPackages()));
    }
    dependencies.stream()
        .filter(d -> !skippedDependencies.contains(d))
        .forEach(
            dep -> {
              pkgActions.add(
                  new CodeTFPackageAction(
                      CodeTFPackageAction.CodeTFPackageActionType.ADD,
                      CodeTFPackageAction.CodeTFPackageActionResult.FAILED,
                      toPackageUrl(dep)));
            });
    return CodemodPackageUpdateResult.from(pkgActions, pkgChanges, unscannableFiles);
  }

  @VisibleForTesting
  static String toPackageUrl(DependencyGAV dependency) {
    return "pkg:maven/"
        + dependency.group()
        + "/"
        + dependency.artifact()
        + "@"
        + dependency.version();
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
