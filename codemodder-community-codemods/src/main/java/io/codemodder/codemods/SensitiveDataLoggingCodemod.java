package io.codemodder.codemods;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import com.google.common.annotations.VisibleForTesting;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.llm.OpenAIGPT35TurboCodeChanger;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A cross-language codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SensitiveDataLoggingCodemod extends OpenAIGPT35TurboCodeChanger {

  @VisibleForTesting
  static final Pattern loggingCallPattern =
      Pattern.compile("log(ger)?.(info|warn|error|fatal)", Pattern.CASE_INSENSITIVE);

  @Override
  protected Set<Integer> findLinesOfInterest(final CodemodInvocationContext context)
      throws IOException {
    List<String> fileContent = Files.readAllLines(context.path());
    List<String> keywords = List.of("password", "secret", "token", "key", "credentials", "ssn");
    Set<Integer> linesToReview = new HashSet<>();
    for (int i = 0; i < fileContent.size(); i++) {
      String line = fileContent.get(i);
      Matcher matcher = loggingCallPattern.matcher(line);
      boolean isMatch =
          matcher.find()
              && keywords.stream().anyMatch(keyword -> containsIgnoreCase(line, keyword));
      if (isMatch) {
        linesToReview.add(i + 1);
      }
    }
    return linesToReview;
  }

  @Override
  protected String getUserPrompt() {
    return "I want to check if this code is logging sensitive data, like passwords, access tokens, API keys, session IDs, SSNs, or something similarly sensitive and remove the log statement if so. Make sure if you fix it, you remove only that statement. I am providing other lines just to give you context. I am worried about string variables.";
  }
}
