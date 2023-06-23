package io.codemodder.codemods;

import io.codemodder.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Codemod(
    id="pixee:java/spring-absolute-cookie-timeout",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    author = "arshan@pixee.ai"
)
public final class SpringAbsoluteCookieTimeoutCodemod extends RawFileChanger {

    private final Duration safeDuration;

    public SpringAbsoluteCookieTimeoutCodemod() {
        this.safeDuration = Duration.ofHours(30);
    }

    @Override
    public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
        Path path = context.path();
        if(!"application.properties".equalsIgnoreCase(path.getFileName().toString())) {
            return List.of();
        }

        List<CodemodChange> changes = new ArrayList<>();
        LineIncludesExcludes lineIncludesExcludes = context.lineIncludesExcludes();
        List<String> lines = Files.readAllLines(path);
        for(int i = 1; i < lines.size()+1; i++) {
            if(!lineIncludesExcludes.matches(i)) {
                continue;
            }
            String line = lines.get(i - 1).trim();
            Matcher matcher = timeoutPattern.matcher(line);
            if(matcher.matches()) {
                try {
                    Duration foundDuration = parseExistingValueFromLine(matcher.group(3), matcher.group(4));
                    if(foundDuration.compareTo(safeDuration) >= 0) {
                        continue;
                    }
                    String parameter = context.stringParameter("timeout", lines.size() + 1, "8h").trim();
                    changes.add(CodemodChange.from(i+1));
                    lines.set(i, "server.servlet.session.timeout=" + parameter);
                } catch (Exception e) {
                    LOG.error("Problem parsing session timeout value from line: `{}`", line);
                }
            }
        }

        if(!changes.isEmpty()) {
            Files.write(path, lines);
            return Collections.unmodifiableList(changes);
        }
        return List.of();
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


    private static final Pattern timeoutPattern = Pattern.compile("server\\.servlet\\.session\\.timeout(\\s)*=(\\s)*(\\d+)([mshdwy])");
    private static final Logger LOG = LoggerFactory.getLogger(SpringAbsoluteCookieTimeoutCodemod.class);
}
