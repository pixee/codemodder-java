package io.openpixee.codetl.cli;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import picocli.CommandLine;

/** Main entrypoint for the CodeTL CLI application. */
@CommandLine.Command(
    name = "codetl",
    mixinStandardHelpOptions = true,
    description = "Automatically transform source code at scale to improve code bases.")
public final class Application implements Callable<Integer> {

  public static void main(final String[] args) {
    final int exitCode = new CommandLine(new Application()).execute(args);
    System.exit(exitCode);
  }

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

  @Override
  public Integer call() {
    final URL jsLanguageProviderBundleURL =
        ClassLoader.getSystemResource("javascript-language-provider.js");
    if (jsLanguageProviderBundleURL == null) {
      throw new NullPointerException(
          "Cannot find js language provider bundle. Check classpath or GraalVM native image resource inclusion configuration");
    }
    final Source jsLanguageProviderBundleSource;
    try {
      jsLanguageProviderBundleSource = Source.newBuilder("js", jsLanguageProviderBundleURL).build();
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Failed to read JS language provider bundle. This should never happen", e);
    }
    try (var context = Context.newBuilder("js").build()) {
      context.eval(jsLanguageProviderBundleSource);
      final var provider = context.eval("js", "codetlProvider.weave");
      try (var paths = Files.walk(repositoryRoot)) {
        // TODO ignore node_modules
        paths
            .filter(Files::isRegularFile)
            .map(
                path -> {
                  final String contents;
                  try {
                    contents = Files.readString(path, StandardCharsets.UTF_8);
                  } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read file " + path, e);
                  }
                  final Value result = provider.execute(path.toString(), contents, "auto");
                  // TODO create CodeTF result object
                  return result.asString();
                })
            .forEach(System.out::println);
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to walk repository " + repositoryRoot, e);
      }
    }
    return 0;
  }
}
