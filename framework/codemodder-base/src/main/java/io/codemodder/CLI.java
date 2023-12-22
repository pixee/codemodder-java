package io.codemodder;

import static io.codemodder.Logs.logEnteringPhase;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.google.common.base.Stopwatch;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFReportGenerator;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserFacade;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import net.logstash.logback.encoder.LogstashEncoder;
import net.logstash.logback.fieldnames.LogstashCommonFieldNames;
import net.logstash.logback.fieldnames.LogstashFieldNames;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;

/** The mixinStandardHelpOptions provides version and help options. */
@CommandLine.Command(
    name = "codemodder",
    mixinStandardHelpOptions = true,
    description = "Run a codemodder codemod")
final class CLI implements Callable<Integer> {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Clock clock;
  private final FileFinder fileFinder;
  private final EncodingDetector encodingDetector;
  private final JavaParserFactory javaParserFactory;
  private final SourceDirectoryLister sourceDirectoryLister;
  private final CodeTFReportGenerator reportGenerator;
  private final String[] args;

  @CommandLine.Option(
      names = {"--output"},
      description = "the output file to produce")
  private File output;

  @CommandLine.Option(
      names = {"--dry-run"},
      description = "do everything except make changes to files",
      defaultValue = "false")
  private boolean dryRun;

  @CommandLine.Option(
      names = {"--max-files"},
      description = "the number of files each codemod can scan",
      defaultValue = "-1")
  private int maxFiles;

  @CommandLine.Option(
      names = {"--max-workers"},
      description = "the maximum number of workers (threads) to use for parallel processing",
      defaultValue = "-1")
  private int maxWorkers;

  @CommandLine.Option(
      names = {"--max-file-size"},
      description = "the maximum file size in bytes that each codemod can scan",
      defaultValue = "-1")
  private int maxFileSize;

  @CommandLine.Option(
      names = {"--dont-exit"},
      description = "dont exit the process after running the codemods",
      hidden = true,
      defaultValue = "false")
  private boolean dontExit;

  @CommandLine.Option(
      names = {"--verbose"},
      description = "print more to stdout",
      defaultValue = "false")
  private boolean verbose;

  @CommandLine.Option(
      names = {"--output-format"},
      description = "the format for the data output file (\"codetf\" or \"diff\")",
      defaultValue = "codetf")
  private OutputFormat outputFormat;

  @CommandLine.Option(
      names = {"--log-format"},
      description = "the format of log data(\"human\" or \"json\")",
      defaultValue = "human")
  private LogFormat logFormat;

  @CommandLine.Option(
      names = {"--project-name"},
      description = "a descriptive name for the project being scanned for reporting")
  private String projectName;

  @CommandLine.Option(
      names = {"--sonar-issues-json"},
      description =
          "a path to a file containing the result of a call to the Sonar Web API Issues endpoint")
  private Path sonarIssuesJsonFilePath;

  @CommandLine.Option(
      names = {"--list"},
      description = "print codemod(s) metadata, then exit",
      defaultValue = "false")
  private boolean listCodemods;

  @CommandLine.Option(
      names = {"--path-include"},
      description = "comma-separated set of UNIX glob patterns to include",
      split = ",")
  private List<String> pathIncludes;

  @CommandLine.Option(
      names = {"--path-exclude"},
      description = "comma-separated set of UNIX glob patterns to exclude",
      split = ",")
  private List<String> pathExcludes;

  @CommandLine.Option(
      names = {"--codemod-include"},
      description = "comma-separated set of codemod IDs to include",
      split = ",")
  private List<String> codemodIncludes;

  @CommandLine.Option(
      names = {"--parameter"},
      description = "a codemod parameter")
  private List<String> codemodParameters;

  @CommandLine.Option(
      names = {"--codemod-exclude"},
      description = "comma-separated set of codemod IDs to exclude",
      split = ",")
  private List<String> codemodExcludes;

  @CommandLine.Parameters(
      arity = "0..1",
      paramLabel = "DIRECTORY",
      description = "the directory to run the codemod on")
  private File projectDirectory;

  @CommandLine.Option(
      names = {"--sarif"},
      description = "comma-separated set of path(s) to SARIF file(s) to feed to the codemods",
      split = ",")
  private List<String> sarifs;

  private final DryRunTempDirCreationStrategy dryRunTempDirCreationStrategy;

  /** The format for the output file. */
  enum OutputFormat {
    CODETF,
    DIFF
  }

  /** The format for the log output. */
  enum LogFormat {
    HUMAN,
    JSON
  }

  CLI(final String[] args, final List<Class<? extends CodeChanger>> codemodTypes) {
    this(
        args,
        codemodTypes,
        Clock.systemUTC(),
        new DefaultFileFinder(),
        new DefaultEncodingDetector(),
        JavaParserFactory.newFactory(),
        SourceDirectoryLister.createDefault(),
        CodeTFReportGenerator.createDefault(),
        new DefaultDryRunTempDirCreationStrategy());
  }

  CLI(
      final String[] args,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Clock clock,
      final FileFinder fileFinder,
      final EncodingDetector encodingDetector,
      final JavaParserFactory javaParserFactory,
      final SourceDirectoryLister sourceDirectoryLister,
      final CodeTFReportGenerator reportGenerator,
      final DryRunTempDirCreationStrategy dryRunTempDirCreationStrategy) {
    Objects.requireNonNull(codemodTypes);
    this.codemodTypes = Collections.unmodifiableList(codemodTypes);
    this.clock = Objects.requireNonNull(clock);
    this.fileFinder = Objects.requireNonNull(fileFinder);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
    this.javaParserFactory = Objects.requireNonNull(javaParserFactory);
    this.sourceDirectoryLister = Objects.requireNonNull(sourceDirectoryLister);
    this.reportGenerator = Objects.requireNonNull(reportGenerator);
    this.args = Objects.requireNonNull(args);
    this.dryRunTempDirCreationStrategy = Objects.requireNonNull(dryRunTempDirCreationStrategy);
  }

  @VisibleForTesting
  static class DefaultFileFinder implements FileFinder {
    @Override
    public List<Path> findFiles(final Path projectDir, final IncludesExcludes includesExcludes) {
      final List<Path> allFiles;
      try (Stream<Path> paths = Files.walk(projectDir)) {
        allFiles =
            paths
                .filter(Files::isRegularFile)
                .filter(
                    p -> !Files.isSymbolicLink(p)) // could cause infinite loop if we follow links
                .filter(p -> includesExcludes.shouldInspect(p.toFile()))
                .sorted()
                .toList();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      return allFiles;
    }
  }

  /** A seam responsible for creating a temp directory for the dry-run feature. */
  @VisibleForTesting
  interface DryRunTempDirCreationStrategy {
    Path createTempDir() throws IOException;
  }

  private static class DefaultDryRunTempDirCreationStrategy
      implements DryRunTempDirCreationStrategy {
    @Override
    public Path createTempDir() throws IOException {
      return Files.createTempDirectory("codemodder-project");
    }
  }

  @Override
  public Integer call() throws IOException {

    if (verbose) {
      setupVerboseLogging();
    }

    if (LogFormat.JSON.equals(logFormat)) {
      setupJsonLogging();
    }

    if (listCodemods) {
      for (Class<? extends CodeChanger> codemodType : codemodTypes) {
        Codemod annotation = codemodType.getAnnotation(Codemod.class);
        log.info(annotation.id());
      }
      return SUCCESS;
    }

    logEnteringPhase(Logs.ExecutionPhase.STARTING);
    log.info("codemodder: java/{}", CLI.class.getPackage().getImplementationVersion());

    if (projectDirectory == null) {
      log.error("No project directory specified");
      return ERROR_CANT_READ_PROJECT_DIRECTORY;
    }

    Path outputPath = null;
    if (output != null) {
      outputPath = output.getAbsoluteFile().toPath();

      // check if the output file parent directory doesn't exist or isn't writable
      if (!Files.exists(outputPath.getParent()) || !Files.isWritable(outputPath.getParent())) {
        log.error("The output file parent directory doesn't exist or isn't writable");
        return ERROR_CANT_WRITE_OUTPUT_FILE;
      }
    }

    Path projectPath = projectDirectory.toPath();
    if (!Files.isDirectory(projectPath) || !Files.isReadable(projectPath)) {
      log.error("The project directory is not a readable directory");
      return ERROR_CANT_READ_PROJECT_DIRECTORY;
    }

    if (maxWorkers < -1) {
      log.error("Invalid value for workers");
      return -1;
    }

    logEnteringPhase(Logs.ExecutionPhase.SETUP);

    if (dryRun) {
      // create a temp dir and copy all the contents into it -- this may be slow for big repos on
      // cloud i/o
      Path copiedProjectDirectory = dryRunTempDirCreationStrategy.createTempDir();
      Stopwatch watch = Stopwatch.createStarted();
      log.debug("dry run temporary directory: {}", copiedProjectDirectory);
      FileUtils.copyDirectory(projectDirectory, copiedProjectDirectory.toFile());
      watch.stop();
      Duration elapsed = watch.elapsed();
      log.debug("dry run copy finished: {}ms", elapsed.toMillis());

      // now that we've copied it, reassign the project directory to that place
      projectDirectory = copiedProjectDirectory.toFile();
      projectPath = copiedProjectDirectory;
    }

    try {
      Instant start = clock.instant();

      // get path includes/excludes
      List<String> pathIncludes = this.pathIncludes;
      if (pathIncludes == null) {
        pathIncludes = defaultPathIncludes;
      }

      List<String> pathExcludes = this.pathExcludes;
      if (pathExcludes == null) {
        pathExcludes = defaultPathExcludes;
      }
      IncludesExcludes includesExcludes =
          IncludesExcludes.withSettings(projectDirectory, pathIncludes, pathExcludes);

      log.debug("including paths: {}", pathIncludes);
      log.debug("excluding paths: {}", pathExcludes);

      // get all files that match
      List<SourceDirectory> sourceDirectories =
          sourceDirectoryLister.listJavaSourceDirectories(List.of(projectDirectory));
      List<Path> filePaths = fileFinder.findFiles(projectPath, includesExcludes);

      // get codemod includes/excludes
      final CodemodRegulator regulator;
      if (codemodIncludes != null && codemodExcludes != null) {
        log.error("Codemod includes and excludes cannot both be specified");
        return ERROR_INVALID_ARGUMENT;
      } else if (codemodIncludes == null && codemodExcludes == null) {
        // the user didn't pass any includes, which means all are enabled
        regulator = CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of());
      } else if (codemodIncludes != null) {
        regulator = CodemodRegulator.of(DefaultRuleSetting.DISABLED, codemodIncludes);
      } else {
        // the user only specified excludes
        regulator = CodemodRegulator.of(DefaultRuleSetting.ENABLED, codemodExcludes);
      }

      // create the loader
      List<Path> sarifFiles =
          sarifs != null ? sarifs.stream().map(Path::of).collect(Collectors.toList()) : List.of();
      Map<String, List<RuleSarif>> pathSarifMap =
          SarifParser.create().parseIntoMap(sarifFiles, projectPath);
      List<ParameterArgument> codemodParameters =
          createFromParameterStrings(this.codemodParameters);
      CodemodLoader loader =
          new CodemodLoader(
              codemodTypes,
              regulator,
              projectPath,
              pathIncludes,
              pathExcludes,
              filePaths,
              pathSarifMap,
              codemodParameters,
              sonarIssuesJsonFilePath);
      List<CodemodIdPair> codemods = loader.getCodemods();

      log.debug("sarif files: {}", sarifFiles.size());

      // create the project providers
      List<ProjectProvider> projectProviders = loadProjectProviders();
      List<CodeTFProvider> codeTFProviders = loadCodeTFProviders();

      List<CodeTFResult> results = new ArrayList<>();

      /*
       * Run the codemods on the files. Note that we cache the compilation units so that we're always retaining
       * the original concrete syntax information (e.g., line numbers) in JavaParser from the first access. This
       * is what allows our codemods to act on SARIF-providing tools data accurately over multiple codemods.
       */
      logEnteringPhase(Logs.ExecutionPhase.SCANNING);

      Provider<JavaParser> javaParserProvider =
          () -> {
            try {
              return javaParserFactory.create(sourceDirectories);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          };
      JavaParserFacade javaParserFacade = JavaParserFacade.from(javaParserProvider);
      int maxFileCacheSize = 10_000;
      FileCache fileCache = FileCache.createDefault(maxFileCacheSize);

      for (CodemodIdPair codemod : codemods) {
        CodemodExecutor codemodExecutor =
            new DefaultCodemodExecutor(
                projectPath,
                includesExcludes,
                codemod,
                projectProviders,
                codeTFProviders,
                fileCache,
                javaParserFacade,
                encodingDetector,
                maxFileSize,
                maxFiles,
                maxWorkers);

        log.info("running codemod: {}", codemod.getId());
        CodeTFResult result = codemodExecutor.execute(filePaths);
        if (!result.getChangeset().isEmpty() || !result.getFailedFiles().isEmpty()) {
          results.add(result);
        }
        if (!result.getChangeset().isEmpty()) {
          log.info("changed:");
          result
              .getChangeset()
              .forEach(
                  entry -> {
                    log.info("  - " + entry.getPath());
                    String indentedDiff =
                        entry
                            .getDiff()
                            .lines()
                            .map(line -> "      " + line)
                            .collect(Collectors.joining(System.lineSeparator()));
                    log.debug("    diff:");
                    log.debug(indentedDiff);
                  });
        }
        if (!result.getFailedFiles().isEmpty()) {
          log.info("failed:");
          result.getFailedFiles().forEach(f -> log.info("  - {}", f));
        }
      }

      Instant end = clock.instant();
      long elapsed = end.toEpochMilli() - start.toEpochMilli();

      logEnteringPhase(Logs.ExecutionPhase.REPORT);
      logMetrics(results);

      // write out the output if they want it
      if (outputPath != null) {
        if (OutputFormat.CODETF.equals(outputFormat)) {
          CodeTFReport report =
              reportGenerator.createReport(
                  projectDirectory.toPath(),
                  String.join(" ", args),
                  sarifs == null
                      ? List.of()
                      : sarifs.stream().map(Path::of).collect(Collectors.toList()),
                  results,
                  elapsed);
          ObjectMapper mapper = new ObjectMapper();
          mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
          Files.writeString(outputPath, mapper.writeValueAsString(report));
          log.debug("report file: {}", outputPath);
        } else if (OutputFormat.DIFF.equals(outputFormat)) {
          throw new UnsupportedOperationException("not supported yet");
        }
      }

      log.debug("elapsed: {}ms", elapsed);

      // this is a special exit code that tells the caller to not exit
      if (dontExit) {
        return -1;
      }

      return SUCCESS;
    } finally {
      if (dryRun) {
        // delete the temp directory
        FileUtils.deleteDirectory(projectDirectory);
        log.debug("cleaned temp directory: {}", projectDirectory);
      }
    }
  }

  /**
   * Performs a resetting of the logging settings because they're wildly different if we do
   * structured logging.
   */
  private void setupJsonLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger rootLogger =
        context.getLogger(LoggingConfigurator.OUR_ROOT_LOGGER_NAME);
    rootLogger.detachAndStopAllAppenders();
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setContext(context);
    configureAppender(appender, Optional.ofNullable(projectName));
    rootLogger.addAppender(appender);
  }

  /**
   * This code is difficult to test because it affects stdout at runtime, and JUnit appears to be
   * mucking with stdout capture.
   */
  @VisibleForTesting
  static void configureAppender(
      final OutputStreamAppender<ILoggingEvent> appender, final Optional<String> projectName) {
    LogstashEncoder logstashEncoder = new LogstashEncoder();
    logstashEncoder.setContext(appender.getContext());

    // we need this to get the caller data, like the file, line, etc.
    logstashEncoder.setIncludeCallerData(true);

    // customize the output to the specification, but include timestamp as well since that's allowed
    LogstashFieldNames fieldNames = logstashEncoder.getFieldNames();
    fieldNames.setCallerFile("file");
    fieldNames.setCallerLine("line");
    fieldNames.setTimestamp("timestamp");
    fieldNames.setCaller(null);
    fieldNames.setCallerClass(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    fieldNames.setCallerMethod(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    fieldNames.setVersion(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    fieldNames.setLogger(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    fieldNames.setThread(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);
    fieldNames.setLevelValue(LogstashCommonFieldNames.IGNORE_FIELD_INDICATOR);

    String projectNameKey = "project_name";
    if (projectName.isPresent()) {
      MDC.put(projectNameKey, projectName.get());
    } else {
      // clear it in case this from tests
      MDC.remove(projectNameKey);
    }

    logstashEncoder.addIncludeMdcKeyName(projectNameKey);
    logstashEncoder.start();
    appender.setEncoder(logstashEncoder);
    appender.start();
  }

  private void setupVerboseLogging() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    ch.qos.logback.classic.Logger rootLogger =
        context.getLogger(LoggingConfigurator.OUR_ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.DEBUG);
  }

  private static void logMetrics(final List<CodeTFResult> results) {
    List<String> failedFiles = results.stream().flatMap(r -> r.getFailedFiles().stream()).toList();

    List<String> changedFiles =
        results.stream()
            .flatMap(r -> r.getChangeset().stream())
            .map(CodeTFChangesetEntry::getPath)
            .toList();

    long uniqueChangedFiles = changedFiles.stream().distinct().count();
    long uniqueFailedFiles = failedFiles.stream().distinct().count();

    log.debug("failed files: {} ({} unique)", failedFiles.size(), uniqueFailedFiles);
    log.debug("changed files: {} ({} unique)", changedFiles.size(), uniqueChangedFiles);
  }

  private List<CodeTFProvider> loadCodeTFProviders() {
    List<CodeTFProvider> codeTFProviders = new ArrayList<>();
    ServiceLoader<CodeTFProvider> loader = ServiceLoader.load(CodeTFProvider.class);
    for (CodeTFProvider provider : loader) {
      codeTFProviders.add(provider);
    }
    return codeTFProviders;
  }

  private List<ProjectProvider> loadProjectProviders() {
    List<ProjectProvider> projectProviders = new ArrayList<>();
    ServiceLoader<ProjectProvider> loader = ServiceLoader.load(ProjectProvider.class);
    for (ProjectProvider projectProvider : loader) {
      projectProviders.add(projectProvider);
    }
    return projectProviders;
  }

  /**
   * Translate the codemod parameters delivered as CLI arguments in the form of LDAP-style
   * name=value pairs into their POJO form.
   */
  private List<ParameterArgument> createFromParameterStrings(final List<String> parameterStrings) {
    if (parameterStrings == null || parameterStrings.isEmpty()) {
      return List.of();
    }
    return parameterStrings.stream().map(ParameterArgument::fromNameValuePairs).toList();
  }

  private static final int SUCCESS = 0;
  private static final int ERROR_CANT_READ_PROJECT_DIRECTORY = 1;
  private static final int ERROR_CANT_WRITE_OUTPUT_FILE = 2;
  private static final int ERROR_INVALID_ARGUMENT = 3;

  private static final List<String> defaultPathIncludes =
      List.of(
          "**.java",
          "**/*.java",
          "pom.xml",
          "**/pom.xml",
          "**.jsp",
          "**/*.jsp",
          "web.xml",
          "**/web.xml");

  private static final List<String> defaultPathExcludes =
      List.of(
          "**/test/**",
          "**/testFixtures/**",
          "**/*Test.java",
          "**/intTest/**",
          "**/tests/**",
          "**/target/**",
          "**/build/**",
          "**/.mvn/**",
          ".mvn/**");

  private static final Logger log = LoggerFactory.getLogger(CLI.class);
}
