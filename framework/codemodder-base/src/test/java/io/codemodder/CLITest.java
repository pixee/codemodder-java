package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFReportGenerator;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Test aspects of {@link CLI}. */
final class CLITest {

  private Path workingRepoDir;
  private Path fooJavaFile;
  private Path barJavaFile;
  private Path testFile;
  private List<SourceDirectory> sourceDirectories;

  @BeforeEach
  void setup(final @TempDir Path tmpDir) throws IOException {
    workingRepoDir = tmpDir;
    Path module1JavaDir =
        Files.createDirectories(tmpDir.resolve("module1/src/alternateMain/java/com/acme/"));
    Path module2JavaDir =
        Files.createDirectories(tmpDir.resolve("module2/src/main/java/com/acme/util/"));
    fooJavaFile = module1JavaDir.resolve("Foo.java");
    barJavaFile = module2JavaDir.resolve("Bar.java");
    testFile = module2JavaDir.resolve("MyTest.java");
    Files.write(
        fooJavaFile,
        "import com.acme.util.Bar; class Foo {private var bar = new Bar();}".getBytes());
    Files.write(barJavaFile, "public class Bar {}".getBytes());
    Files.write(testFile, "public class MyTest {}".getBytes());

    /*
     * Only add module2 to the discovered source directories. This will help prove that the module1 files can still be seen and changed, even if we couldn't locate it as a "source directory".
     */
    sourceDirectories =
        List.of(
            SourceDirectory.createDefault(
                tmpDir.resolve("module2/src/main/java").toAbsolutePath(),
                List.of(barJavaFile.toAbsolutePath().toString())));
  }

  @Test
  void it_works_normally() throws IOException {
    Path outputFile = Files.createTempFile("codetf", ".json");
    String[] args =
        new String[] {"--dont-exit", "--output", outputFile.toString(), workingRepoDir.toString()};
    Runner.run(List.of(Cloud9Changer.class), args);
    assertThat(Files.readString(fooJavaFile)).contains("cloud9");
    assertThat(Files.exists(outputFile)).isTrue();
  }

  @Test
  void it_doesnt_change_test_file() throws IOException {
    String[] args = new String[] {"--dont-exit", workingRepoDir.toString()};
    Runner.run(List.of(Cloud9Changer.class), args);
    assertThat(Files.readString(testFile)).doesNotContain("cloud9");
  }

  @Test
  void it_works_without_output_file() throws IOException {
    String[] args = new String[] {"--dont-exit", workingRepoDir.toString()};
    Runner.run(List.of(Cloud9Changer.class), args);
    assertThat(Files.readString(fooJavaFile)).contains("cloud9");
  }

  /**
   * Runs the CLI with arguments that we know will cause it to fail, and asserts that the exit code
   * denotes failure and an error message is captured in the alternative stderr stream.
   */
  @Test
  void it_captures_console_output() {
    final var args = new String[] {"--bogus"};
    final var stdoutWriter = new StringWriter();
    final var stderrWriter = new StringWriter();
    try (var stdout = new PrintWriter(stdoutWriter);
        var stderr = new PrintWriter(stderrWriter)) {
      final var code = Runner.run(List.of(Cloud9Changer.class), args, stdout, stderr);
      assertThat(code).isEqualTo(2);
    }
    assertThat(stderrWriter.toString()).startsWith("Unknown option: '--bogus'");
  }

  @Test
  void structured_appender_works_with_project_name() throws IOException {
    Path outputFile = Files.createTempFile("codemodder", ".out");
    writeLogMessageToFile(outputFile, "cloud9");
    String output = Files.readString(outputFile);
    assertThat(output).contains("\"project_name\":\"cloud9\"");
  }

  @Test
  void structured_appender_works_without_project_name() throws IOException {
    Path outputFile = Files.createTempFile("codemodder", ".out");
    writeLogMessageToFile(outputFile, null);
    String output = Files.readString(outputFile);
    assertThat(output).doesNotContain("cloud9");
  }

  private static void writeLogMessageToFile(final Path outputFile, final String projectName) {
    LoggerContext context = new LoggerContext();
    FileAppender<ILoggingEvent> appender = new FileAppender<>();
    appender.setContext(context);

    appender.setFile(outputFile.toAbsolutePath().toString());
    appender.setImmediateFlush(true);
    CLI.configureAppender(appender, Optional.ofNullable(projectName));
    Logger log = context.getLogger("myRootLogger");
    log.addAppender(appender);
    log.info("this is a test");
  }

  @Test
  void dry_run_works() throws IOException {
    Path normalCodetf = Files.createTempFile("normal", ".codetf");
    Path dryRunCodetf = Files.createTempFile("dryrun", ".codetf");

    // call with dry-run and confirm it doesn't change
    {
      String[] args =
          new String[] {
            "--dont-exit",
            "--dry-run",
            "--output",
            dryRunCodetf.toString(),
            workingRepoDir.toString()
          };
      Runner.run(List.of(Cloud9Changer.class), args);
      assertThat(Files.readString(fooJavaFile)).doesNotContain("cloud9");
    }

    // call without dry-run and confirm it changes as expected
    {
      String[] args =
          new String[] {
            "--dont-exit", "--output", normalCodetf.toString(), workingRepoDir.toString()
          };
      Runner.run(List.of(Cloud9Changer.class), args);
      assertThat(Files.readString(fooJavaFile)).contains("cloud9");
    }

    // deeply inspect the codetf and assert that they're the same
    ObjectMapper mapper = new ObjectMapper();
    CodeTFReport normalCodeTFReport = mapper.readValue(normalCodetf.toFile(), CodeTFReport.class);
    CodeTFReport dryRunCodeTFReport = mapper.readValue(dryRunCodetf.toFile(), CodeTFReport.class);

    List<CodeTFResult> normalCodeTFResults = normalCodeTFReport.getResults();
    List<CodeTFResult> dryRunCodeTFResults = dryRunCodeTFReport.getResults();

    // we just compare the results because the "run" section is non-deterministic (has elapsed time
    // etc.)
    String normalJson = mapper.writeValueAsString(normalCodeTFResults);
    String dryRunJson = mapper.writeValueAsString(dryRunCodeTFResults);
    assertThat(normalJson).isEqualTo(dryRunJson);
  }

  @Test
  void dry_run_failure_still_deletes_temp() throws IOException {
    Path tmpDir = Files.createTempDirectory("dry-run-test");
    CLI.DryRunTempDirCreationStrategy tmpDirStrategy = () -> tmpDir;
    Path codetf = Files.createTempFile("codetf", ".json");

    String[] args = {
      "--dont-exit", "--dry-run", "--output", codetf.toString(), workingRepoDir.toString()
    };
    SourceDirectoryLister sourceDirectoryLister = mock(SourceDirectoryLister.class);
    when(sourceDirectoryLister.listJavaSourceDirectories(any(List.class)))
        .thenThrow(new IllegalArgumentException("intentional error"));
    CLI cli =
        new CLI(
            args,
            List.of(Cloud9Changer.class),
            Clock.systemDefaultZone(),
            new CLI.DefaultFileFinder(),
            EncodingDetector.create(),
            JavaParserFactory.newFactory(),
            sourceDirectoryLister,
            CodeTFReportGenerator.createDefault(),
            tmpDirStrategy);
    CommandLine commandLine = new CommandLine(cli).setCaseInsensitiveEnumValuesAllowed(true);
    int code = commandLine.execute(args);
    assertThat(code).isEqualTo(1);
    assertThat(Files.notExists(tmpDir)).isTrue();
  }

  @Test
  void file_finder_works() throws IOException {
    FileFinder finder = new CLI.DefaultFileFinder();

    IncludesExcludes all = IncludesExcludes.any();
    List<Path> files = finder.findFiles(workingRepoDir, all);
    assertThat(files).containsExactly(fooJavaFile, barJavaFile, testFile);

    IncludesExcludes onlyFoo =
        IncludesExcludes.withSettings(workingRepoDir.toFile(), List.of("**/Foo.java"), List.of());
    files = finder.findFiles(workingRepoDir, onlyFoo);
    assertThat(files).containsExactly(fooJavaFile);
  }

  /** This codemod just replaces Java file contents with the word "cloud9" */
  @Codemod(
      id = "org:java/cloud9",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
      importance = Importance.LOW)
  private static class Cloud9Changer extends RawFileChanger {
    private Cloud9Changer() {
      super(UselessReportStrategy.INSTANCE);
    }

    @Override
    public List<CodemodChange> visitFile(final CodemodInvocationContext context)
        throws IOException {
      Path path = context.path();
      if (path.toString().endsWith(".java")) {
        Files.writeString(path, "cloud9");
        return List.of(CodemodChange.from(1));
      }
      return List.of();
    }
  }

  @Test
  void javaparser_factory_works() throws IOException {
    JavaParserFactory factory = JavaParserFactory.newFactory();
    JavaParser javaParser = factory.create(sourceDirectories);
    Optional<CompilationUnit> result = javaParser.parse(fooJavaFile).getResult();
    assertThat(result.isPresent()).isTrue();
    CompilationUnit cu = result.get();
    assertThat(cu.getTypes()).hasSize(1);
    assertThat(cu.getTypes().get(0).getNameAsString()).isEqualTo("Foo");
  }
}
