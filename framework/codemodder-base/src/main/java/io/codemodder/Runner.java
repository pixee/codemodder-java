package io.codemodder;

import java.io.PrintWriter;
import java.util.List;
import picocli.CommandLine;

/** An entrypoint for the codemodder framework. */
public final class Runner {

  /**
   * Runs the default, Pixee-developed codemods.
   *
   * @param codemods The codemods to run
   * @param args The arguments to pass to the codemod runner
   * @param stdout alternate stdout to use, or {@code null} to use the default
   * @param stderr alternate stderr to use, or {@code null} to use the default
   * @return the exit code of the codemodder CLI
   */
  public static int run(
      final List<Class<? extends CodeChanger>> codemods,
      final String[] args,
      final PrintWriter stdout,
      final PrintWriter stderr) {
    CommandLine commandLine =
        new CommandLine(new CLI(args, codemods)).setCaseInsensitiveEnumValuesAllowed(true);
    if (stdout != null) {
      commandLine.setOut(stdout);
    }
    if (stderr != null) {
      commandLine.setErr(stderr);
    }
    return commandLine.execute(args);
  }

  /**
   * @see #run(List, String[], PrintWriter, PrintWriter)
   */
  public static void run(final List<Class<? extends CodeChanger>> codemods, final String[] args) {
    final int code = run(codemods, args, null, null);
    // we honor a special exit code (-1) that tells us not to exit, this is useful for integration
    // tests
    if (code != -1) {
      System.exit(code);
    }
  }
}
