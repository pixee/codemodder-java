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
  private final List<CodeTFProvider> codetfProviders;
  private final CachingJavaParser cachingJavaParser;
  private final Path projectDir;
  private final IncludesExcludes includesExcludes;
  private final EncodingDetector encodingDetector;
  private final FileCache fileCache;

  DefaultCodemodExecutor(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final List<CodeTFProvider> codetfProviders,
      final FileCache fileCache,
      final CachingJavaParser cachingJavaParser,
      final EncodingDetector encodingDetector) {
    this.projectDir = Objects.requireNonNull(projectDir);
    this.includesExcludes = Objects.requireNonNull(includesExcludes);
    this.codemod = Objects.requireNonNull(codemod);
    this.codetfProviders = Objects.requireNonNull(codetfProviders);
    this.projectProviders = Objects.requireNonNull(projectProviders);
    this.cachingJavaParser = Objects.requireNonNull(cachingJavaParser);
    this.fileCache = Objects.requireNonNull(fileCache);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
  }

  @Override
  public CodeTFResult execute(final List<Path> filePaths) {
    List<CodeTFChangesetEntry> changeset = new ArrayList<>();
    Set<Path> unscannableFiles = new HashSet<>();
    DefaultCodeDirectory codeDirectory = new DefaultCodeDirectory(projectDir);
    CodeChanger codeChanger = codemod.getChanger();

    /*
     *  Create the right CodemodRunner based on the type of CodeChanger.
     */
    CodemodRunner codemodRunner;
    if (codeChanger instanceof JavaParserChanger) {
      codemodRunner =
          new JavaParserCodemodRunner(
              cachingJavaParser, (JavaParserChanger) codeChanger, encodingDetector);
    } else if (codeChanger instanceof RawFileChanger) {
      codemodRunner = new RawFileCodemodRunner((RawFileChanger) codeChanger);
    } else {
      throw new UnsupportedOperationException(
          "unsupported codeChanger type: " + codeChanger.getClass().getName());
    }

    /*
     * Filter the files to those that the CodemodRunner supports.
     */
    List<Path> codemodTargetFiles =
        filePaths.stream().filter(codemodRunner::supports).sorted().toList();

    for (Path filePath : codemodTargetFiles) {
      // create the context necessary for the codemod to run
      LineIncludesExcludes lineIncludesExcludes =
          includesExcludes.getIncludesExcludesForFile(filePath.toFile());

      try {
        String fileContents = fileCache.get(filePath);
        CodemodInvocationContext context =
            new DefaultCodemodInvocationContext(
                codeDirectory, filePath, fileContents, codemod.getId(), lineIncludesExcludes);

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
                              codeChanger, filePath, change, pkgActions))
                  .collect(Collectors.toList());

          // make sure we add the file's entry first, then the dependency entries, so the causality
          // is clear
          List<String> beforeFile = fileContents.lines().toList();
          String afterContents = Files.readString(filePath);
          List<String> afterFile = afterContents.lines().toList();
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
          fileCache.overrideEntry(filePath, afterContents);
        }
      } catch (Exception e) {
        unscannableFiles.add(filePath);
        e.printStackTrace();
      }
    }

    CodeTFResult result =
        new CodeTFResult(
            codemod.getId(),
            codeChanger.getSummary(),
            codeChanger.getDescription(),
            unscannableFiles.stream()
                .map(file -> getRelativePath(projectDir, file))
                .collect(Collectors.toSet()),
            codeChanger.getReferences(),
            emptyMap(),
            changeset);

    for (CodeTFProvider provider : codetfProviders) {
      result = provider.onResultCreated(result);
    }
    return result;
  }

  @NotNull
  private CodeTFChange translateCodemodChangetoCodeTFChange(
      final CodeChanger codeChanger,
      final Path filePath,
      final CodemodChange codemodChange,
      final List<CodeTFPackageAction> pkgActions) {
    Optional<String> customizedChangeDescription = codemodChange.getDescription();
    String changeDescription =
        customizedChangeDescription.orElse(
            codeChanger.getIndividualChangeDescription(filePath, codemodChange));
    CodeTFChange change =
        new CodeTFChange(
            codemodChange.lineNumber(),
            emptyMap(),
            changeDescription,
            pkgActions,
            codemodChange.getParameters());

    for (CodeTFProvider provider : codetfProviders) {
      change = provider.onChangeCreated(filePath, codemod.getId(), change);
    }
    return change;
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
      unscannableFiles.addAll(result.erroredFiles().stream().map(Path::toAbsolutePath).toList());
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
    String path = projectDir.relativize(filePath).toString();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return path;
  }
}
