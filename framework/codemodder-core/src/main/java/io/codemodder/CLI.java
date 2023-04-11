package io.codemodder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** The mixinStandardHelpOptions provides version and help options. */
@CommandLine.Command(
    name = "codemodder",
    mixinStandardHelpOptions = true,
    description = "Run a codemodder codemod")
final class CLI implements Callable<Integer> {

  private final List<Class<? extends Changer>> codemods;

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

  CLI(final List<Class<? extends Changer>> codemods) {
    Objects.requireNonNull(codemods);
    this.codemods = Collections.unmodifiableList(codemods);
  }

  @Override
  public Integer call() {

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

    // get path includes/excludes
    if (pathIncludes == null) {
      pathIncludes = defaultPathIncludes;
    }

    if (pathExcludes == null) {
      pathExcludes = defaultPathExcludes;
    }
    IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(projectDirectory, pathIncludes, pathExcludes);

    // get all files that match

    // run the codemods on the files
    CodemodRegulator regulator = CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of());
    CodemodInvoker invoker = new CodemodInvoker(codemods, regulator, projectPath);
    FileWeavingContext context = FileWeavingContext.createDefault();

    //        //CodeTFGenerator reportGenerator = new CodeTFGenerator();
    //        CodeTFReport report =
    //                reportGenerator.createReport(
    //                        repositoryRoot, includePatterns, excludePatterns, allWeaveResults,
    // elapsed);

    // write out the JSON
    //        ObjectMapper mapper = new ObjectMapper();
    //        FileUtils.write(output, mapper.writeValueAsString(report), StandardCharsets.UTF_8);

    return SUCCESS;
  }

  enum OutputFormat {
    CODETF,
    DIFF
  }

  private static final int ERROR_CANT_READ_PROJECT_DIRECTORY = 1;
  private static final int ERROR_CANT_WRITE_OUTPUT_FILE = 2;
  private static final int SUCCESS = 0;

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
