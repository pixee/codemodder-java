package io.codemodder;

import java.util.List;
import picocli.CommandLine;

/** An entrypoint for the codemodder framework. */
public final class Runner {

  /**
   * Runs the codemods built in the codemodder framework.
   *
   * @param codemods The codemods to run
   * @param args The arguments to pass to the codemod runner
   */
  public static void run(final List<Class<? extends CodeChanger>> codemods, final String[] args) {
    CommandLine commandLine =
        new CommandLine(new CLI(args, codemods)).setCaseInsensitiveEnumValuesAllowed(true);
    int exitCode = commandLine.execute(args);

    // we honor a special exit code (-1) that tells us not to exit, this is useful for integration
    // tests
    if (exitCode != -1) {
      System.exit(exitCode);
    }
  }
}
