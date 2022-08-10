package io.pixee.codefixer.java;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS;

import com.github.lalyos.jfiglet.FigletFont;
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = "javafixit",
    mixinStandardHelpOptions = true,
    description = "scans a repository with suggested weaves for Java")
public final class JavaFixitCli implements Callable<Integer> {

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

  public static void main(final String[] args) throws IOException {
    String banner = FigletFont.convertOneLine("open-pixee");
    System.out.println(banner);
    final int exitCode = new CommandLine(new JavaFixitCli()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    int rc;
    try {
      JavaFixitCliRun cliRun = new JavaFixitCliRun();
      cliRun.run(
          ruleDefault != null ? ruleDefault : DefaultRuleSetting.ENABLED,
          ruleExceptions != null ? ruleExceptions : Collections.emptyList(),
          sarifs != null ? sarifs : Collections.emptyList(),
          repositoryRoot,
          includes != null ? includes : Collections.emptyList(),
          excludes != null ? excludes : Collections.emptyList(),
          output,
          verbose);
      stopwatch.stop();

      Logger log = LoggerFactory.getLogger(JavaFixitCli.class);
      log.info("Weaved repository in {}", formatDurationHMS(stopwatch.elapsed().toMillis()));
      rc = cliSuccessCode;
    } catch (Exception e) {
      e.printStackTrace();
      rc = cliErrorCode;
    }
    return rc;
  }

  private static class DefaultRuleSettingConverter
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

  private static final int cliSuccessCode = 0;
  private static final int cliErrorCode = -1;
}
