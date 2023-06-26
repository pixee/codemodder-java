package io.codemodder.codemods;

import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Codemod(
    id = "pixee:java/spring-absolute-cookie-timeout",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    author = "arshan@pixee.ai")
public final class SpringAbsoluteCookieTimeoutCodemod extends RawFileChanger {

  private final Duration safeDuration;
  private final Parameter timeout;

  @Inject
  public SpringAbsoluteCookieTimeoutCodemod(
      final @CodemodParameter(
              name = "timeout",
              description = "Absolute timeout value for Spring session cookies",
              defaultValue = "8h",
              validationPattern = "\\d+[smhd]") Parameter timeout) {
    this.timeout = timeout;
    String defaultValue = timeout.getDefaultValue();
    String count = defaultValue.substring(0, defaultValue.length() - 1);
    String units = defaultValue.substring(defaultValue.length() - 1);
    this.safeDuration = parseExistingValueFromLine(count, units);
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
    Path path = context.path();
    if (!"application.properties".equalsIgnoreCase(path.getFileName().toString())) {
      return List.of();
    } else if (!inExpectedDir(context.codeDirectory().asPath().relativize(path))) {
      return List.of();
    }

    List<CodemodChange> changes = new ArrayList<>();
    LineIncludesExcludes lineIncludesExcludes = context.lineIncludesExcludes();
    List<String> lines = Files.readAllLines(path);
    boolean mustAdd = true;
    for (int currentLine = 1; currentLine < lines.size() + 1; currentLine++) {
      if (!lineIncludesExcludes.matches(currentLine)) {
        continue;
      }
      String line = lines.get(currentLine - 1).trim();
      Matcher matcher = timeoutPattern.matcher(line);
      if (matcher.matches()) {
        try {
          Duration foundDuration = parseExistingValueFromLine(matcher.group(3), matcher.group(4));
          if (foundDuration.compareTo(safeDuration) <= 0) {
            mustAdd = false;
            continue;
          }
          String parameter = timeout.getValue(path, currentLine);
          changes.add(CodemodChange.from(currentLine));
          lines.set(currentLine - 1, "server.servlet.session.timeout=" + parameter);
          mustAdd = false;
        } catch (Exception e) {
          LOG.error("Problem parsing session timeout value from line: `{}`", line);
        }
      }
    }

    if (mustAdd) {
      String parameter = timeout.getValue(path, lines.size());
      changes.add(CodemodChange.from(lines.size()));
      List<String> newLines = new ArrayList<>(lines);
      newLines.add("server.servlet.session.timeout=" + parameter);
      lines = newLines;
    }

    if (!changes.isEmpty()) {
      Files.write(path, lines);
      return Collections.unmodifiableList(changes);
    }
    return List.of();
  }

  private boolean inExpectedDir(final Path relativePath) {
    return relativePath.toString().contains("src/main/resources");
  }

  private Duration parseExistingValueFromLine(final String number, final String unit) {
    long value = Integer.parseInt(number);
    return switch (unit) {
      case "m" -> Duration.ofMinutes(value);
      case "s" -> Duration.ofSeconds(value);
      case "h" -> Duration.ofHours(value);
      case "d" -> Duration.ofDays(value);
      case "w" -> Duration.ofDays(value * 7);
      case "y" -> Duration.ofDays(value * 365);
      default -> throw new IllegalArgumentException("Unknown unit: " + unit);
    };
  }

  private static final Pattern timeoutPattern =
      Pattern.compile("server\\.servlet\\.session\\.timeout(\\s)*=(\\s)*(\\d+)([mshdwy])");
  private static final Logger LOG =
      LoggerFactory.getLogger(SpringAbsoluteCookieTimeoutCodemod.class);
}
