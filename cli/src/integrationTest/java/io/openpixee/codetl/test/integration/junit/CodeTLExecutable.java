package io.openpixee.codetl.test.integration.junit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Helper for integration tests that exec a CodeTL application and verify its results.
 *
 * <p>Because this type encapsulates <em>how</em> CodeTL is run, this type is the seam between
 * integration tests that need to run CodeTL and the test framework that determines how CodeTL
 * should be run. It provides us the flexibility to change CodeTL how the tests run CodeTL (JVM
 * application, native-image, container) without needing to rewrite the integration tests.
 */
public final class CodeTLExecutable {

  private final Path executable;

  /**
   * @param executable path to the codetl executable to exec
   */
  CodeTLExecutable(final Path executable) {
    this.executable = Objects.requireNonNull(executable);
  }

  /**
   * Executes the CodeTL program with the given arguments and waits for it to complete.
   *
   * @param args CodeTL program arguments
   * @throws IOException when there is an IO error while executing the program or reading its output
   * @throws InterruptedException when interrupted while waiting for CodeTL executable to complete
   */
  public void execute(final String... args) throws IOException, InterruptedException {
    final var process = newProcessBuilder(args);
    final var err = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    final int code = process.waitFor();
    if (code != 0) {
      throw new CodeTLExecutionException(code, err);
    }
  }

  /**
   * Executes the CodeTL program with the given arguments and returns the resulting {@link Process}
   * handle.
   *
   * @param args CodeTL program arguments
   * @return new process handle
   * @throws IOException when there is an IO error while executing the program
   */
  Process newProcessBuilder(final String... args) throws IOException {
    final var command = new ArrayList<String>(args.length + 1);
    command.add(executable.toString());
    command.addAll(Arrays.asList(args));
    return new ProcessBuilder(command).start();
  }
}
