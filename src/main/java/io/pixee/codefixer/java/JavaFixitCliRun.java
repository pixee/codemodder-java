package io.pixee.codefixer.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pixee.codetf.CodeTFReport;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
      final File output)
      throws IOException {

    LOG.info("Default rule setting: {}", defaultRuleSetting.getDescription());
    LOG.info("Exceptions to the default rule setting: {}", ruleExceptions);
    LOG.info("Repository path: {}", repositoryRoot);
    LOG.info("Output: {}", output);
    LOG.info("Includes: {}", includePatterns.size());
    LOG.info("Excludes: {}", excludePatterns.size());

    final StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    // find the java source directories -- avoid test directories or other incidental java code
    LOG.info("Scanning for Java source directories");
    final List<SourceDirectory> sourceDirectories =
        directorySearcher.listJavaSourceDirectories(List.of(repositoryRoot));
    LOG.info("Scanning {} Java source directories", sourceDirectories.size());

    // parse the includes & exclude rules we'll need for all the scanning
    final IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(repositoryRoot, includePatterns, excludePatterns);

    // get the Java code visitors
    RuleContext ruleContext = RuleContext.of(defaultRuleSetting, ruleExceptions);
    final List<VisitorFactory> factories =
        visitorAssembler.assembleJavaCodeScanningVisitorFactories(
            repositoryRoot, ruleContext, sarifs);

    // run the Java code visitors
    final var javaSourceWeaveResult =
        javaSourceWeaver.weave(sourceDirectories, factories, includesExcludes);

    // get the non-Java code visitors
    final List<FileBasedVisitor> fileBasedVisitors =
        visitorAssembler.assembleFileVisitors(ruleContext);

    // run the non-Java code visitors
    final var fileBasedWeaveResults =
        fileWeaver.weave(
            fileBasedVisitors, repositoryRoot, javaSourceWeaveResult, includesExcludes);

    // merge the results into one
    final var allWeaveResults = merge(javaSourceWeaveResult, fileBasedWeaveResults);
    LOG.info(
        "Analysis complete ({} changes suggested, {} errors encountered)",
        allWeaveResults.changedFiles().size(),
        allWeaveResults.unscannableFiles().size());

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

  private static final Logger LOG = LogManager.getLogger(JavaFixitCliRun.class);
}
