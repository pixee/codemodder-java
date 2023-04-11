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
  public static void run(final List<Class<? extends Changer>> codemods, final String[] args) {
    CommandLine commandLine =
        new CommandLine(new CLI(codemods)).setCaseInsensitiveEnumValuesAllowed(true);
    int exitCode = commandLine.execute(args);
    System.exit(exitCode);
  }
}
