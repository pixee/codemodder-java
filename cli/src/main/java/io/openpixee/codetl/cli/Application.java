package io.openpixee.codetl.cli;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS;

import ch.qos.logback.classic.Level;
import io.openpixee.codetl.cli.logging.LoggingConfigurator;
import io.openpixee.codetl.config.DefaultRuleSetting;
import io.openpixee.java.JavaFixitCliRun;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** Main entrypoint for the CodeTL CLI application. */
@CommandLine.Command(
    name = "codetl",
    mixinStandardHelpOptions = true,
    description = "Automatically transform source code at scale to improve code bases.")
public final class Application implements Callable<Integer> {

  public static void main(final String[] args) throws IOException {
    //    String banner = FigletFont.convertOneLine("open-pixee");
    //    System.out.println(banner);
    final int exitCode = new CommandLine(new Application()).execute(args);
    System.exit(exitCode);
  }

  @CommandLine.Option(
      names = {"-r", "--repository"},
      description = "Source code repository path",
      required = true)
  private File repositoryRoot;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Specify the file to write the output results to",
      required = true)
  private File output;

  @CommandLine.Option(
      names = {"-d", "--rule-default"},
      description = "Specify the default rule setting ('enabled' or 'disabled')",
      defaultValue = "enabled",
      converter = DefaultRuleSettingConverter.class,
      required = false)
  private DefaultRuleSetting ruleDefault;

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
  private List<File> sarifs;

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
    final StopWatch stopwatch = StopWatch.createStarted();
    if (verbose) {
      enableDebugLogging();
    }
    final JavaFixitCliRun cliRun = new JavaFixitCliRun();
    try {
      cliRun.run(
          ruleDefault != null ? ruleDefault : DefaultRuleSetting.ENABLED,
          ruleExceptions != null ? ruleExceptions : Collections.emptyList(),
          sarifs != null ? sarifs : Collections.emptyList(),
          repositoryRoot,
          includes != null ? includes : defaultIncludes,
          excludes != null ? excludes : defaultExcludes,
          output);
      stopwatch.stop();

      Logger log = LoggerFactory.getLogger(Application.class);
      log.info("Weaved repository in {}", formatDurationHMS(stopwatch.getTime()));
      return cliSuccessCode;
    } catch (Exception e) {
      e.printStackTrace(System.err);
      return cliErrorCode;
    }
  }

  /** Dynamically raises the log level to DEBUG for more output! */
  private void enableDebugLogging() {
    ch.qos.logback.classic.Logger rootLogger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(LoggingConfigurator.OUR_ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel("DEBUG"));
  }

  private static final class DefaultRuleSettingConverter
      implements CommandLine.ITypeConverter<DefaultRuleSetting> {
    @Override
    public DefaultRuleSetting convert(final String s) {
      if ("enabled".equalsIgnoreCase(s)) {
        return DefaultRuleSetting.ENABLED;
      } else if ("disabled".equalsIgnoreCase(s)) {
        return DefaultRuleSetting.DISABLED;
      }
      throw new IllegalArgumentException(
          "invalid setting for default rule setting -- must be 'enabled' (default) or 'disabled'");
    }
  }

  @VisibleForTesting
  static final List<String> defaultIncludes =
      List.of(
          "**.java",
          "**/*.java",
          "pom.xml",
          "**/pom.xml",
          "**.jsp",
          "**/*.jsp",
          "web.xml",
          "**/web.xml");

  @VisibleForTesting static final List<String> defaultExcludes = List.of("**/test/**");

  private static final int cliSuccessCode = 0;
  private static final int cliErrorCode = -1;
}
