package io.codemodder.codemods;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import com.google.common.annotations.VisibleForTesting;
import com.theokanning.openai.service.OpenAiService;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Line;
import io.codemodder.ReviewGuidance;
import io.codemodder.plugins.llm.OpenAIGPT35TurboCodeChanger;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import javax.inject.Inject;

/** A cross-language codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SensitiveDataLoggingCodemod extends OpenAIGPT35TurboCodeChanger {

  @Inject
  public SensitiveDataLoggingCodemod(final OpenAiService openAIService) {
    super(openAIService);
  }

  @Override
  protected IntStream findLinesOfInterest(final CodemodInvocationContext context)
      throws IOException {
    return context
        .lines()
        .filter(
            line ->
                loggingCallPattern.matcher(line.content()).find()
                    && KEYWORDS.stream()
                        .anyMatch(keyword -> containsIgnoreCase(line.content(), keyword)))
        .mapToInt(Line::number);
  }

  @Override
  protected String getUserPrompt() {
    return "I want to check if this code is logging sensitive data, like passwords, access tokens, API keys, session IDs, SSNs, or something similarly sensitive and remove the log statement if so. Make sure if you fix it, you remove only that statement. I am providing other lines just to give you context. I am worried about string variables.";
  }

  @VisibleForTesting
  static final Pattern loggingCallPattern =
      Pattern.compile("log(ger)?.(info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

  private static final List<String> KEYWORDS =
      List.of("password", "secret", "token", "key", "credentials", "ssn");
}
