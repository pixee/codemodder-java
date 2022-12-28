package io.openpixee.codetl.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** Main entrypoint for the CodeTL CLI application. */
@CommandLine.Command(
    name = "codetl",
    mixinStandardHelpOptions = true,
    description = "Automatically transform source code at scale to improve code bases.")
public final class Application implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-r", "--repository"},
      description = "Source code repository path",
      required = true)
  private Path repositoryRoot;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Specify the file to write the output results to",
      required = true)
  private Path output;

  @CommandLine.Option(
      names = {"-x", "--rule-exception"},
      description =
          "Specify the rules that should have have the opposite of the default rule setting",
      required = false)
  private List<String> ruleExceptions;

  @CommandLine.Option(
      names = {"-s", "--sarif"},
      description = "Specify the paths to SARIFs that the hardener should act on",
      required = false)
  private List<Path> sarifs;

  @CommandLine.Option(
      names = {"-i", "--include"},
      description = "Specify the paths to include within the repository",
      required = false)
  private List<String> includes;

  @CommandLine.Option(
      names = {"-e", "--exclude"},
      description = "Specify the paths to exclude within the repository",
      required = false)
  private List<String> excludes;

  @CommandLine.Option(
      names = {"-v", "--verbose"},
      description = "Specify whether debug logging should be enabled",
      defaultValue = "false",
      required = false)
  private boolean verbose;

  public static void main(final String[] args) {
    final int exitCode = new CommandLine(new Application()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    return 0;
  }
}
