package io.openpixee.java;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.*;
import io.codemodder.codemods.DefaultCodemods;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFReportGenerator;
import io.codemodder.plugins.maven.MavenProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is the connective tissue between the process startup and the weaving process. */
public final class JavaFixitCliRun {

  private final SourceDirectoryLister directorySearcher;
  private final SourceWeaver javaSourceWeaver;
  private final VisitorAssembler visitorAssembler;
  private final CodeTFReportGenerator reportGenerator;

  public JavaFixitCliRun() {
    this.directorySearcher = SourceDirectoryLister.createDefault();
    this.javaSourceWeaver = SourceWeaver.createDefault();
    this.visitorAssembler = VisitorAssembler.createDefault();
    this.reportGenerator = CodeTFReportGenerator.createDefault();
  }

  /**
   * Performs the main logic of the application.
   *
   * <p>Note that includes and excludes can both be specified, and "includes win" unless the exclude
   * has a longer matching path.
   *
   * @param defaultRuleSetting the default setting for every rule
   * @param ruleExceptions the rules that should be considered to have the opposite default rule
   *     setting
   * @param sarifs the paths to SARIF files that tool
   * @param repositoryRoot the path to the repository root
   * @param includePatterns the patterns to include, null and empty list means all files are scanned
   * @param excludePatterns the patterns to exclude, null and empty list means all files are scanned
   * @param output the output file path
   * @return the CCF report describing the scan/patch process we undertook
   */
  public CodeTFReport run(
      final DefaultRuleSetting defaultRuleSetting,
      final List<String> ruleExceptions,
      final List<File> sarifs,
      final File repositoryRoot,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final File output,
      final boolean verbose)
      throws IOException {

    if (verbose) {
      enableDebugLogging();
    }

    LOG.debug("Default rule setting: {}", defaultRuleSetting.getDescription());
    LOG.debug("Exceptions to the default rule setting: {}", ruleExceptions);
    LOG.debug("Repository path: {}", repositoryRoot);
    LOG.debug("Output: {}", output);
    LOG.debug("Includes: {}", includePatterns);
    LOG.debug("Excludes: {}", excludePatterns);

    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // find the java source directories -- avoid test directories or other incidental java code
    LOG.debug("Scanning for Java source directories");

    // parse the includes & exclude rules we'll need for all the scanning
    final IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(repositoryRoot, includePatterns, excludePatterns);

    final List<SourceDirectory> sourceDirectories =
        directorySearcher.listJavaSourceDirectories(List.of(repositoryRoot));

    sourceDirectories.forEach(
        sourceDirectory -> {
          LOG.debug("Scanning Java source directory: {}", sourceDirectory.path());
          LOG.debug("Scanning Java source files from that directory: {}", sourceDirectory.files());
        });

    // get the Java code visitors
    CodemodRegulator codemodRegulator = CodemodRegulator.of(defaultRuleSetting, ruleExceptions);
    final List<VisitorFactory> factories =
        visitorAssembler.assembleJavaCodeScanningVisitorFactories(
            repositoryRoot, codemodRegulator, sarifs);

    List<String> allJavaFiles = new ArrayList<>();
    sourceDirectories.forEach(
        sourceDirectory ->
            allJavaFiles.addAll(
                sourceDirectory.files().stream()
                    .filter(file -> includesExcludes.shouldInspect(new File(file)))
                    .collect(Collectors.toList())));

    LOG.debug("Scanning following files: {}", allJavaFiles.size());

    Map<String, List<RuleSarif>> ruleSarifByTool =
        new SarifParser.Default().parseIntoMap(sarifs, repositoryRoot.toPath());

    List<Class<? extends Changer>> defaultCodemodTypes = DefaultCodemods.asList();
    CodemodLoader codemodInvoker =
        new CodemodLoader(
            defaultCodemodTypes, codemodRegulator, repositoryRoot.toPath(), ruleSarifByTool);

    // run the Java code visitors
    WeavingResult javaSourceWeaveResult =
        javaSourceWeaver.weave(
            repositoryRoot,
            sourceDirectories,
            allJavaFiles,
            factories,
            codemodInvoker,
            includesExcludes);

    WeavingResult codemodRawFileResults =
        invokeRawFileCodemods(codemodInvoker, repositoryRoot.toPath(), List.of(), includesExcludes);

    Set<ChangedFile> preDependencyCombinedChangedFiles = new HashSet<>();
    preDependencyCombinedChangedFiles.addAll(javaSourceWeaveResult.changedFiles());
    preDependencyCombinedChangedFiles.addAll(codemodRawFileResults.changedFiles());

    // build the needed for the new dependency updater we'll use with the codemods
    List<FileDependency> fileDependencies =
        preDependencyCombinedChangedFiles.stream()
            .map(
                file ->
                    FileDependency.create(
                        Path.of(file.originalFilePath()),
                        file.changes().stream()
                            .map(CodemodChange::getDependenciesNeeded)
                            .flatMap(List::stream)
                            .collect(Collectors.toList())))
            .collect(Collectors.toList());

    // update the dependencies
    MavenProvider mavenProvider = new MavenProvider();
    DependencyUpdateResult dependencyUpdate =
        mavenProvider.updateDependencies(
            repositoryRoot.toPath(), preDependencyCombinedChangedFiles, fileDependencies);
    Set<ChangedFile> finalChangedFiles = dependencyUpdate.packageChanges();
    Set<String> finalUnscannableFiles = new HashSet<>();
    finalUnscannableFiles.addAll(
        dependencyUpdate.erroredFiles().stream()
            .map(Path::toAbsolutePath)
            .map(Path::toString)
            .collect(Collectors.toUnmodifiableList()));
    finalUnscannableFiles.addAll(javaSourceWeaveResult.unscannableFiles());
    finalUnscannableFiles.addAll(codemodRawFileResults.unscannableFiles());

    // merge the both the file results and the dependency results into one
    final var allWeaveResults =
        WeavingResult.createDefault(finalChangedFiles, finalUnscannableFiles);
    final var changesCount =
        allWeaveResults.changedFiles().stream()
            .map(changedFile -> changedFile.changes().size())
            .mapToInt(Integer::intValue)
            .sum();
    LOG.info("Analysis complete!");
    LOG.info("\t{} files changed", allWeaveResults.changedFiles().size());
    LOG.info("\t{} changes suggested", changesCount);
    LOG.info("\t{} errors", allWeaveResults.unscannableFiles().size());

    LOG.debug("\n");
    List<ChangedFile> sortedChangedFiles = new ArrayList<>(allWeaveResults.changedFiles());
    sortedChangedFiles.sort(Comparator.comparing(ChangedFile::originalFilePath));

    for (ChangedFile changedFile : sortedChangedFiles) {
      LOG.debug("File: {}", changedFile.originalFilePath());
      List<CodemodChange> sortedWeaves = new ArrayList<>(changedFile.changes());
      sortedWeaves.sort(Comparator.comparing(CodemodChange::lineNumber));
      for (CodemodChange weave : sortedWeaves) {
        LOG.debug("\tLine: {}", weave.lineNumber());
        LOG.debug("\tRule: {}", weave.changeCode());
        LOG.debug("\tDependencies required: {}", weave.getDependenciesNeeded());
        LOG.debug("\n");
      }
    }

    // clean up
    stopWatch.stop();
    final long elapsed = stopWatch.getTime();

    CodeTFReport report =
        reportGenerator.createReport(
            repositoryRoot.toPath(),
            "cmd line",
            sarifs.stream().map(File::toPath).collect(Collectors.toList()),
            List.of(),
            elapsed);

    // write out the JSON
    ObjectMapper mapper = new ObjectMapper();
    FileUtils.write(output, mapper.writeValueAsString(report), StandardCharsets.UTF_8);
    return report;
  }

  /**
   * Invoke {@link io.codemodder.Codemod} types that offer raw file transformation, making sure not
   * to alter any files that have already been modified by legacy {@link FileBasedVisitor} types.
   */
  private WeavingResult invokeRawFileCodemods(
      final CodemodLoader codemodInvoker,
      final Path repositoryRoot,
      final List<Path> filesAlreadyChanged,
      final IncludesExcludes includesExcludes) {
    Set<ChangedFile> changedFiles = new HashSet<>();
    Set<String> unscannableFiles = new HashSet<>();

    try (Stream<Path> stream = Files.walk(repositoryRoot, Integer.MAX_VALUE)) {
      List<File> files =
          stream
              .filter(filesAlreadyChanged::contains)
              .map(Path::toFile)
              .filter(file -> !file.getName().toLowerCase().endsWith(".java"))
              .filter(includesExcludes::shouldInspect)
              .sorted()
              .collect(Collectors.toList());

      for (File filePath : files) {
        var canonicalFile = filePath.getCanonicalFile();
        var context =
            CodemodChangeRecorder.createDefault(
                includesExcludes.getIncludesExcludesForFile(filePath));
        Optional<ChangedFile> changedFile =
            codemodInvoker.executeFile(canonicalFile.toPath(), context);
        changedFile.ifPresent(changedFiles::add);
      }
    } catch (IOException e) {
      LOG.error("Problem scanning repository files with codemods", e);
    }

    return WeavingResult.createDefault(changedFiles, unscannableFiles);
  }

  /** Dynamically raises the log level to DEBUG for more output! */
  private void enableDebugLogging() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(LoggingConfigurator.OUR_ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel("DEBUG"));
  }

  private static final Logger LOG = LoggerFactory.getLogger(JavaFixitCliRun.class);
}
