package io.codemodder;

import static java.util.Collections.emptyMap;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFDiffSide;
import io.codemodder.codetf.CodeTFPackageAction;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.javaparser.JavaParserCodemodRunner;
import io.codemodder.javaparser.JavaParserFacade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultCodemodExecutor implements CodemodExecutor {

  private final CodemodIdPair codemod;
  private final List<ProjectProvider> projectProviders;
  private final List<CodeTFProvider> codetfProviders;
  private final JavaParserFacade javaParserFacade;
  private final Path projectDir;
  private final IncludesExcludes includesExcludes;
  private final EncodingDetector encodingDetector;
  private final FileCache fileCache;

  /** The max size of a file we'll scan. If a file is larger than this, we'll skip it. */
  private final int maxFileSize;

  /**
   * The max number of files we'll scan. If there are more than this number of files, we'll skip
   * them.
   */
  private final int maxFiles;

  /** The max number of workers we'll use to scan files. */
  private final int maxWorkers;

  DefaultCodemodExecutor(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final List<CodeTFProvider> codetfProviders,
      final FileCache fileCache,
      final JavaParserFacade javaParserFacade,
      final EncodingDetector encodingDetector,
      final int maxFileSize,
      final int maxFiles,
      final int maxWorkers) {
    this.projectDir = Objects.requireNonNull(projectDir);
    this.includesExcludes = Objects.requireNonNull(includesExcludes);
    this.codemod = Objects.requireNonNull(codemod);
    this.codetfProviders = Objects.requireNonNull(codetfProviders);
    this.projectProviders = Objects.requireNonNull(projectProviders);
    this.javaParserFacade = Objects.requireNonNull(javaParserFacade);
    this.fileCache = Objects.requireNonNull(fileCache);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
    this.maxFileSize = maxFileSize;
    this.maxFiles = maxFiles;
    this.maxWorkers = maxWorkers;
  }

  @Override
  public CodeTFResult execute(final List<Path> filePaths) {

    Set<Path> unscannableFiles = new ConcurrentSkipListSet<>();
    DefaultCodeDirectory codeDirectory = new DefaultCodeDirectory(projectDir);
    CodeChanger codeChanger = codemod.getChanger();

    /*
     *  Create the right CodemodRunner based on the type of CodeChanger.
     */
    CodemodRunner codemodRunner;
    if (codeChanger instanceof JavaParserChanger) {
      codemodRunner =
          new JavaParserCodemodRunner(
              javaParserFacade, (JavaParserChanger) codeChanger, encodingDetector);
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
        filePaths.stream()
            .filter(codemodRunner::supports)
            .sorted()
            .limit(maxFiles != -1 ? maxFiles : Long.MAX_VALUE)
            .sorted()
            .toList();

    /*
     * The changeset doesn't need to be thread-safe because it's only added to within a synchronized block.
     */
    List<CodeTFChangesetEntry> changeset = new ArrayList<>();

    int workers = maxWorkers != -1 ? maxWorkers : 1;
    ExecutorService executor = Executors.newFixedThreadPool(workers);
    CompletionService<String> service = new ExecutorCompletionService<>(executor);

    // for each file, add a task to the completion service
    for (Path filePath : codemodTargetFiles) {

      executor.submit(
          () -> {
            // create the context necessary for the codemod to run
            LineIncludesExcludes lineIncludesExcludes =
                includesExcludes.getIncludesExcludesForFile(filePath.toFile());

            try {

              if (maxFileSize != -1) {
                long size = Files.size(filePath);
                if (size > maxFileSize) {
                  unscannableFiles.add(filePath);
                  return;
                }
              }

              String beforeFileContents = fileCache.get(filePath);
              CodemodInvocationContext context =
                  new DefaultCodemodInvocationContext(
                      codeDirectory,
                      filePath,
                      beforeFileContents,
                      codemod.getId(),
                      lineIncludesExcludes);

              // run the codemod on the file
              List<CodemodChange> codemodChanges = codemodRunner.run(context);

              if (!codemodChanges.isEmpty()) {
                synchronized (this) {
                  FilesUpdateResult updateResult =
                      updateFiles(codeChanger, filePath, beforeFileContents, codemodChanges);
                  unscannableFiles.addAll(updateResult.filesFailedToChange());
                  changeset.addAll(updateResult.changeset());
                }
              }

            } catch (Exception e) {
              unscannableFiles.add(filePath);
              log.error("Problem scanning file", e);
            }
          });
    }

    executor.shutdown();
    try {
      boolean success = executor.awaitTermination(10, TimeUnit.MINUTES);
      log.trace("Success running codemod: {}", success);
      while (!executor.isTerminated()) {
        final Future<String> future = service.poll(5, TimeUnit.SECONDS);
        if (future != null) {
          log.trace("Finished: {}", future.get());
        }
      }
    } catch (Exception e) {
      log.error("Problem waiting for scanning threads to exit", e);
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

  /**
   * This file method does the hard work of updating files, based on a list of codemod changes
   * reported to have occurred.
   */
  private FilesUpdateResult updateFiles(
      final CodeChanger codeChanger,
      final Path filePath,
      final String beforeFileContents,
      final List<CodemodChange> codemodChanges)
      throws IOException {

    List<Path> filesFailedToChange = List.of();

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
      filesFailedToChange = new ArrayList<>(packageAddResult.filesFailedToChange());
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
                    translateCodemodChangetoCodeTFChange(codeChanger, filePath, change, pkgActions))
            .collect(Collectors.toList());

    // make sure we add the file's entry first, then the dependency entries, so the causality
    // is clear
    List<String> beforeFile = beforeFileContents.lines().toList();
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

    // create a changeset for this file change + its downstream dependency changes
    List<CodeTFChangesetEntry> changeset = new ArrayList<>();
    changeset.add(new CodeTFChangesetEntry(getRelativePath(projectDir, filePath), diff, changes));
    changeset.addAll(dependencyChangesetEntries);

    // update the cache
    fileCache.overrideEntry(filePath, afterContents);
    dependencyChangesetEntries.forEach(
        entry -> fileCache.removeEntry(projectDir.resolve(entry.getPath())));

    return new FilesUpdateResult(changeset, filesFailedToChange);
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
            CodeTFDiffSide.LEFT,
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

  /** Describes the results of updating files after a codemod execution. */
  private record FilesUpdateResult(
      List<CodeTFChangesetEntry> changeset, List<Path> filesFailedToChange) {}

  private static final Logger log = LoggerFactory.getLogger(DefaultCodemodExecutor.class);
}
