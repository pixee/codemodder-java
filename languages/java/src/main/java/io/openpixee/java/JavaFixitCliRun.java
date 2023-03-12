package io.openpixee.java;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.ChangedFile;
import io.codemodder.Changer;
import io.codemodder.CodemodInvoker;
import io.codemodder.DefaultRuleSetting;
import io.codemodder.IncludesExcludes;
import io.codemodder.RuleContext;
import io.codemodder.Weave;
import io.codemodder.codemods.DefaultCodemods;
import io.codemodder.codemods.SecureRandomCodemod;
import io.github.pixee.codetf.CodeTFReport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is the connective tissue between the process startup and the weaving process. */
public final class JavaFixitCliRun {

  private final SourceDirectoryLister directorySearcher;
  private final SourceWeaver javaSourceWeaver;
  private final FileWeaver fileWeaver;
  private final VisitorAssembler visitorAssembler;
  private final CodeTFReportGenerator reportGenerator;

  public JavaFixitCliRun() {
    this.directorySearcher = SourceDirectoryLister.createDefault();
    this.javaSourceWeaver = SourceWeaver.createDefault();
    this.fileWeaver = FileWeaver.createDefault();
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
    RuleContext ruleContext = RuleContext.of(defaultRuleSetting, ruleExceptions);
    final List<VisitorFactory> factories =
        visitorAssembler.assembleJavaCodeScanningVisitorFactories(
            repositoryRoot, ruleContext, sarifs);

    List<String> allJavaFiles = new ArrayList<>();
    sourceDirectories.forEach(
        sourceDirectory ->
            allJavaFiles.addAll(
                sourceDirectory.files().stream()
                    .filter(file -> includesExcludes.shouldInspect(new File(file)))
                    .collect(Collectors.toList())));

    LOG.debug("Scanning following files: {}", allJavaFiles.size());

    List<Class<? extends Changer>> defaultCodemodTypes = DefaultCodemods.asList();
    CodemodInvoker codemodInvoker =
        new CodemodInvoker(
            List.of(SecureRandomCodemod.class), ruleContext, repositoryRoot.toPath());
    // run the Java code visitors
    final var javaSourceWeaveResult =
        javaSourceWeaver.weave(
            repositoryRoot,
            sourceDirectories,
            allJavaFiles,
            factories,
            codemodInvoker,
            includesExcludes);

    // get the non-Java code visitors
    final List<FileBasedVisitor> fileBasedVisitors =
        visitorAssembler.assembleFileVisitors(repositoryRoot, ruleContext, sarifs);

    // run the non-Java code visitors
    final var fileBasedWeaveResults =
        fileWeaver.weave(
            fileBasedVisitors, repositoryRoot, javaSourceWeaveResult, includesExcludes);

    // merge the results into one
    final var allWeaveResults = merge(javaSourceWeaveResult, fileBasedWeaveResults);
    final var changesCount =
        allWeaveResults.changedFiles().stream()
            .map(changedFile -> changedFile.weaves().size())
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
      List<Weave> sortedWeaves = new ArrayList<>(changedFile.weaves());
      sortedWeaves.sort(Comparator.comparing(Weave::lineNumber));
      for (Weave weave : sortedWeaves) {
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
            repositoryRoot, includePatterns, excludePatterns, allWeaveResults, elapsed);

    // write out the JSON
    ObjectMapper mapper = new ObjectMapper();
    FileUtils.write(output, mapper.writeValueAsString(report), StandardCharsets.UTF_8);
    return report;
  }

  /** Dynamically raises the log level to DEBUG for more output! */
  private void enableDebugLogging() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(LoggingConfigurator.OUR_ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel("DEBUG"));
  }

  /**
   * When we need to combine the results of multiple analyses, we can combine them with a method
   * like this one. There is notably some loss of fidelity here when the two sets are combined, but
   * hopefully all the changers have different domains over different types of files, so there
   * should be little chance of collision.
   */
  private WeavingResult merge(final WeavingResult result1, final WeavingResult result2) {
    var combinedChangedFiles = new HashSet<ChangedFile>();
    combinedChangedFiles.addAll(result1.changedFiles());
    combinedChangedFiles.addAll(result2.changedFiles());

    var combinedUnscannableFiles = new HashSet<String>();
    combinedUnscannableFiles.addAll(result1.unscannableFiles());
    combinedUnscannableFiles.addAll(result2.unscannableFiles());

    return WeavingResult.createDefault(combinedChangedFiles, combinedUnscannableFiles);
  }

  private static final Logger LOG = LoggerFactory.getLogger(JavaFixitCliRun.class);
}
