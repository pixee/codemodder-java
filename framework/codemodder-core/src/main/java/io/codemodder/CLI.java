package io.codemodder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFReportGenerator;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  /** The format for the output file. */
  enum OutputFormat {
    CODETF,
    DIFF
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
        CodeTFReportGenerator.createDefault());
  }

  CLI(
      final String[] args,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Clock clock,
      final FileFinder fileFinder,
      final EncodingDetector encodingDetector,
      final JavaParserFactory javaParserFactory,
      final SourceDirectoryLister sourceDirectoryLister,
      final CodeTFReportGenerator reportGenerator) {
    Objects.requireNonNull(codemodTypes);
    this.codemodTypes = Collections.unmodifiableList(codemodTypes);
    this.clock = Objects.requireNonNull(clock);
    this.fileFinder = Objects.requireNonNull(fileFinder);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
    this.javaParserFactory = Objects.requireNonNull(javaParserFactory);
    this.sourceDirectoryLister = Objects.requireNonNull(sourceDirectoryLister);
    this.reportGenerator = Objects.requireNonNull(reportGenerator);
    this.args = Objects.requireNonNull(args);
  }

  @VisibleForTesting
  static class DefaultFileFinder implements FileFinder {
    @Override
    public List<Path> findFiles(
        final List<SourceDirectory> sourceDirectories, final IncludesExcludes includesExcludes) {
      List<Path> allFiles = new ArrayList<>();
      for (SourceDirectory directory : sourceDirectories) {
        allFiles.addAll(
            directory.files().stream()
                .map(File::new)
                .filter(includesExcludes::shouldInspect)
                .map(File::toPath)
                .filter(
                    f -> !Files.isSymbolicLink(f)) // could cause infinite loop if we follow links
                .sorted()
                .collect(Collectors.toList()));
      }
      return allFiles;
    }
  }

  @Override
  public Integer call() throws IOException {

    if (listCodemods) {
      for (Class<? extends CodeChanger> codemodType : codemodTypes) {
        Codemod annotation = codemodType.getAnnotation(Codemod.class);
        log.info(annotation.id());
      }
      return SUCCESS;
    }

    if (output == null) {
      log.error("The output file is required");
      return ERROR_CANT_WRITE_OUTPUT_FILE;
    }

    if (projectDirectory == null) {
      log.error("No project directory specified");
      return ERROR_CANT_READ_PROJECT_DIRECTORY;
    }

    Path projectPath = projectDirectory.toPath();
    if (!Files.isDirectory(projectPath) || !Files.isReadable(projectPath)) {
      log.error("The project directory is not a readable directory");
      return ERROR_CANT_READ_PROJECT_DIRECTORY;
    }
    Path outputPath = output.toPath();
    if (!Files.isWritable(outputPath) && !Files.isWritable(outputPath.getParent())) {
      log.error("The output file (or its parent directory) is not writable");
      return ERROR_CANT_WRITE_OUTPUT_FILE;
    }

    if (dryRun) {
      throw new UnsupportedOperationException("Dry run is not yet supported");
    }

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

    // get all files that match
    List<SourceDirectory> sourceDirectories =
        sourceDirectoryLister.listJavaSourceDirectories(List.of(projectDirectory));
    List<Path> filePaths = fileFinder.findFiles(sourceDirectories, includesExcludes);

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
    CodemodLoader loader = new CodemodLoader(codemodTypes, regulator, projectPath);
    List<CodemodIdPair> codemods = loader.getCodemods();

    // create the project providers
    List<ProjectProvider> projectProviders = loadProjectProviders();

    // create the JavaParser instance

    List<CodeTFResult> results = new ArrayList<>();

    /*
     * Run the codemods on the files. Note that we cache the compilation units so that we're always retaining
     * the original concrete syntax information (e.g., line numbers) in JavaParser from the first access. This
     * is what allows our codemods to act on SARIF-providing tools data accurately over multiple codemods.
     */
    JavaParser javaParser = javaParserFactory.create(sourceDirectories);
    CachingJavaParser cachingJavaParser = CachingJavaParser.from(javaParser);
    for (CodemodIdPair codemod : codemods) {
      CodemodExecutor codemodExecutor =
          new DefaultCodemodExecutor(
              projectPath,
              includesExcludes,
              codemod,
              projectProviders,
              cachingJavaParser,
              encodingDetector);
      CodeTFResult result = codemodExecutor.execute(filePaths);
      if (!result.getChangeset().isEmpty() || !result.getFailedFiles().isEmpty()) {
        results.add(result);
      }
    }

    Instant end = clock.instant();
    long elapsed = end.toEpochMilli() - start.toEpochMilli();

    // write out the output
    if (OutputFormat.CODETF.equals(outputFormat)) {
      CodeTFReport report =
          reportGenerator.createReport(
              projectDirectory.toPath(),
              String.join(" ", args),
              sarifs.stream().map(Path::of).collect(Collectors.toList()),
              results,
              elapsed);
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      Files.write(outputPath, mapper.writeValueAsString(report).getBytes(StandardCharsets.UTF_8));
    } else if (OutputFormat.DIFF.equals(outputFormat)) {
      throw new UnsupportedOperationException("not supported yet");
    }

    return SUCCESS;
  }

  private List<ProjectProvider> loadProjectProviders() {
    List<ProjectProvider> projectProviders = new ArrayList<>();
    ServiceLoader<ProjectProvider> loader = ServiceLoader.load(ProjectProvider.class);
    for (ProjectProvider projectProvider : loader) {
      projectProviders.add(projectProvider);
    }
    return projectProviders;
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
      List.of("**/test/**", "**/target/**", "**/build/**");

  private static final Logger log = LoggerFactory.getLogger(CLI.class);
}
