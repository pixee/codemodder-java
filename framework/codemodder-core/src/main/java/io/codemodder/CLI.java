package io.codemodder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFReportGenerator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;
import org.mozilla.universalchardet.UniversalDetector;
import picocli.CommandLine;

/** The mixinStandardHelpOptions provides version and help options. */
@CommandLine.Command(
    name = "codemodder",
    mixinStandardHelpOptions = true,
    description = "Run a codemodder codemod")
final class CLI implements Callable<Integer> {

  private final List<Class<? extends Changer>> codemods;
  private final Clock clock;
  private final FileFinder fileFinder;
  private final JavaParserFactory javaParserFactory;
  private final EncodingDetector encodingDetector;
  private final SourceDirectoryLister sourceDirectoryLister;

  @CommandLine.Option(
      names = {"--output"},
      description = "the output file to produce",
      required = true)
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
      arity = "1",
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

  CLI(final List<Class<? extends Changer>> codemods) {
    this(
        codemods,
        Clock.systemUTC(),
        new DefaultFileFinder(),
        new DefaultJavaParserFactory(),
        new DefaultEncodingDetector(),
        SourceDirectoryLister.createDefault());
  }

  CLI(
      final List<Class<? extends Changer>> codemods,
      final Clock clock,
      final FileFinder fileFinder,
      final JavaParserFactory javaParserFactory,
      final EncodingDetector encodingDetector,
      final SourceDirectoryLister sourceDirectoryLister) {
    Objects.requireNonNull(codemods);
    this.codemods = Collections.unmodifiableList(codemods);
    this.clock = Objects.requireNonNull(clock);
    this.fileFinder = Objects.requireNonNull(fileFinder);
    this.javaParserFactory = Objects.requireNonNull(javaParserFactory);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
    this.sourceDirectoryLister = Objects.requireNonNull(sourceDirectoryLister);
  }

  @VisibleForTesting
  static class DefaultEncodingDetector implements EncodingDetector {
    @Override
    public Optional<String> detect(final Path originalJavaFile) throws IOException {
      String encoding = UniversalDetector.detectCharset(originalJavaFile);
      return Optional.ofNullable(encoding);
    }
  }

  @VisibleForTesting
  static class DefaultJavaParserFactory implements JavaParserFactory {

    @Override
    public JavaParser create(final List<SourceDirectory> sourceDirectories) throws IOException {
      final JavaParser javaParser = new JavaParser();
      final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
      combinedTypeSolver.add(new ReflectionTypeSolver());
      sourceDirectories.forEach(
          javaDirectory -> combinedTypeSolver.add(new JavaParserTypeSolver(javaDirectory.path())));
      javaParser
          .getParserConfiguration()
          .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
      return javaParser;
    }
  }

  @VisibleForTesting
  static class DefaultFileFinder implements FileFinder {
    @Override
    public List<Path> findFiles(
        final List<SourceDirectory> sourceDirectories, final IncludesExcludes includesExcludes)
        throws IOException {
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
      for (Class<? extends Changer> codemod : codemods) {
        Codemod annotation = codemod.getAnnotation(Codemod.class);
        System.out.println(annotation.id());
      }
      return SUCCESS;
    }

    Path projectPath = projectDirectory.toPath();
    if (!Files.isDirectory(projectPath) || !Files.isReadable(projectPath)) {
      System.err.println("The project directory is not a readable directory");
      return ERROR_CANT_READ_PROJECT_DIRECTORY;
    }
    Path outputPath = output.toPath();
    if (!Files.isWritable(outputPath) && !Files.isWritable(outputPath.getParent())) {
      System.err.println("The output file (or its parent directory) is not writable");
      return ERROR_CANT_WRITE_OUTPUT_FILE;
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
      System.err.println("Codemod includes and excludes cannot both be specified");
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

    // create the invoker
    CodemodInvoker invoker = new CodemodInvoker(codemods, regulator, projectPath);

    // run the codemods on the files
    Set<ChangedFile> changedFiles = new HashSet<>();
    Set<String> unscannableFiles = new HashSet<>();

    // java files get fed to the codemodder apis that handle java compilation units, everything else
    // gets sent to the codemodder apis that handle raw files
    JavaParser javaParser = javaParserFactory.create(sourceDirectories);
    for (Path filePath : filePaths) {
      File file = filePath.toFile();
      FileWeavingContext fileContext = FileWeavingContext.createDefault(file, includesExcludes);
      Optional<ChangedFile> changedFile = Optional.empty();
      try {
        if (file.getName().toLowerCase().endsWith(".java")) {
          changedFile = invokeOnJavaFile(invoker, javaParser, filePath, fileContext);
        } else {
          changedFile = invoker.executeFile(filePath, fileContext);
        }
      } catch (Exception e) {
        System.err.println("Error processing file " + file.getAbsolutePath());
        unscannableFiles.add(file.getAbsolutePath());
      }
      changedFile.ifPresent(changedFiles::add);
    }

    /*
     * Now that all the codemods have had a chance to touch all the files, we can ask the project providers to apply
     * any corrective changers to the project as a whole. This is useful for things like adding a dependency to the
     * project's pom.xml file.
     */
    List<FileDependency> fileDependencies = new ArrayList<>();
    for (ChangedFile file : changedFiles) {
      Path path = Path.of(file.modifiedFile());
      List<DependencyGAV> deps =
          file.weaves().stream()
              .map(Weave::getDependenciesNeeded)
              .flatMap(Collection::stream)
              .distinct()
              .collect(Collectors.toList());
      fileDependencies.add(FileDependency.create(path, deps));
    }

    List<ProjectProvider> projectProviders = loadProjectProviders();
    for (ProjectProvider projectProvider : projectProviders) {
      DependencyUpdateResult result =
          projectProvider.updateDependencies(projectPath, changedFiles, fileDependencies);
      unscannableFiles.addAll(
          result.erroredFiles().stream()
              .map(Path::toAbsolutePath)
              .map(Objects::toString)
              .collect(Collectors.toList()));
      changedFiles = result.updatedChanges();
      fileDependencies.removeAll(result.injectedDependencies());
    }

    WeavingResult results = WeavingResult.createDefault(changedFiles, unscannableFiles);

    Instant end = clock.instant();
    long elapsed = end.toEpochMilli() - start.toEpochMilli();

    // write out the output
    if (OutputFormat.CODETF.equals(outputFormat)) {
      CodeTFReportGenerator reportGenerator = CodeTFReportGenerator.createDefault();
      CodeTFReport report =
          reportGenerator.createReport(
              projectDirectory, pathIncludes, pathExcludes, results, elapsed);
      ObjectMapper mapper = new ObjectMapper();
      Files.write(outputPath, mapper.writeValueAsString(report).getBytes(StandardCharsets.UTF_8));
    } else if (OutputFormat.DIFF.equals(outputFormat)) {
      throw new UnsupportedOperationException("not supported yet");
    }

    if (!dryRun) {
      // write out the changed files
      for (ChangedFile changedFile : results.changedFiles()) {
        byte[] contents = Files.readAllBytes(Path.of(changedFile.modifiedFile()));
        Files.write(Path.of(changedFile.originalFilePath()), contents);
      }
    } else {
      System.out.println("Dry run, not writing any files");
    }

    return SUCCESS;
  }

  private Optional<ChangedFile> invokeOnJavaFile(
      final CodemodInvoker invoker,
      final JavaParser javaParser,
      final Path file,
      final FileWeavingContext context)
      throws IOException {
    Optional<ChangedFile> changedFile = Optional.empty();
    final InputStream in = Files.newInputStream(file);
    final ParseResult<CompilationUnit> result = javaParser.parse(in);
    if (!result.isSuccessful()) {
      throw new RuntimeException("can't parse file");
    }
    final CompilationUnit cu = result.getResult().orElseThrow();

    // send the compilation unit to the codemodder api
    invoker.execute(file, cu, context);

    // if the file was modified, write it out to a temp file and create a ChangedFile
    if (context.madeWeaves()) {
      final String encoding = encodingDetector.detect(file).orElse("UTF-8");
      final String modified = (LexicalPreservingPrinter.print(cu));
      Path modifiedFile = Files.createTempFile("modified", ".java");
      Files.write(modifiedFile, modified.getBytes(encoding));
      ChangedFile cf =
          ChangedFile.createDefault(
              file.toAbsolutePath().toString(),
              modifiedFile.toAbsolutePath().toString(),
              context.weaves());
      changedFile = Optional.of(cf);
    }
    return changedFile;
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
}
