package io.openpixee.java.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke tests that simply verify that the CLI can process a directory of sources without failing.
 */
final class CLISmokeIT {

  /**
   * Runs the CLI against a set of sources
   *
   * <ol>
   *   <li>Copies sources from src/test/com/acme/testcode into a temporary directory that looks
   *       enough like a Maven java project for the CLI to process the sources.
   *   <li>Uses {@link ProcessBuilder} to run a new java process with the Java language provider
   *       JAR.
   *   <li>Simply verifies that the process exits successfully.
   * </ol>
   *
   * @param tmp temporary directory for transform output
   * @throws IllegalStateException when the environment has not been configured with the requisite
   *     configuration including JAVA_HOME env var and the io.openpixee.test.language-provider-jar
   *     and io.openpixee.test.test-code-dir system properties
   */
  @Test
  void process_java_sources_without_failing(@TempDir final Path tmp, final TestReporter reporter) {
    copyTestCodeSourcesToDirectory(tmp);

    final String javaHome = System.getenv().get("JAVA_HOME");
    if (javaHome == null) {
      throw new IllegalStateException("Expected JAVA_HOME to be set");
    }
    final Path java = Path.of(javaHome).resolve("bin/java");
    if (!(Files.isRegularFile(java) && Files.isExecutable(java))) {
      throw new IllegalStateException(
          "Expected " + java + " to be an executable file. Check JAVA_HOME configuration");
    }
    final Path jar =
        Optional.ofNullable(System.getProperty("io.openpixee.test.language-provider-jar"))
            .map(Path::of)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Expected io.openpixee.test.language-provider-jar to be a path to the CodeTL Java language provider JAR"));
    if (!Files.isRegularFile(jar)) {
      throw new IllegalStateException("Expected " + jar + " to be a regular file");
    }
    final ProcessBuilder builder =
        new ProcessBuilder()
            .command(
                java.toString(),
                "-jar",
                jar.toString(),
                "--repository=" + tmp,
                "--output=" + tmp.resolve("output.codetf.json"))
            .directory(tmp.toFile())
            .inheritIO();
    reporter.publishEntry("Executing language provider: " + builder.command());

    final Process process;
    try {
      process = builder.start();
    } catch (final IOException e) {
      throw (AssertionError) fail("Failed to invoke CodeTL Java language provider jar", e);
    }
    try {
      process.waitFor(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      throw (AssertionError) fail("Timeout waiting for CodeTL to execute", e);
    }

    assertThat(process.exitValue(), equalTo(0));
  }

  /**
   * Helper method that copies the test sources specified by io.openpixee.test.test-code-dir to the
   * given directory.
   *
   * @param dir directory to which the sources will be copied
   * @throws IllegalStateException when the requisite configuration for discovering the test sources
   *     does not exist
   * @throws UncheckedIOException when fails to copy the sources to the given directory
   */
  private static void copyTestCodeSourcesToDirectory(final Path dir) {
    final Path testCodeDir =
        Optional.ofNullable(System.getProperty("io.openpixee.test.test-code-dir"))
            .map(Path::of)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Expected io.openpixee.test.test-code-dir to be a path to test sources for CodeTL to transform"));
    if (!Files.isDirectory(testCodeDir)) {
      throw new IllegalStateException(
          "Expected " + testCodeDir + " to be a directory of Java source files to transform");
    }

    final Path tempSources = dir.resolve("src/main/java");
    try {
      Files.createDirectories(tempSources);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to set-up test sources directory", e);
    }
    try (Stream<Path> sources = Files.walk(testCodeDir)) {
      sources.forEach(
          source -> {
            final Path relativize = testCodeDir.getParent().relativize(source);
            final Path destination = tempSources.resolve(relativize);
            try {
              Files.copy(source, destination);
            } catch (IOException e) {
              throw new UncheckedIOException("Failed to copy file", e);
            }
          });
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to set-up test sources directory", e);
    }
  }
}
