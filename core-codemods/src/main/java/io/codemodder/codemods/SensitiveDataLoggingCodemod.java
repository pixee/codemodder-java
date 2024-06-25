package io.codemodder.codemods;

import io.codemodder.*;
import io.codemodder.plugins.llm.CodeChangingLLMRemediationOutcome;
import io.codemodder.plugins.llm.NoActionLLMRemediationOutcome;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.SarifToLLMForMultiOutcomeCodemod;
import io.codemodder.plugins.llm.StandardModel;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import javax.inject.Inject;

/** A codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SensitiveDataLoggingCodemod extends SarifToLLMForMultiOutcomeCodemod {

  @Inject
  public SensitiveDataLoggingCodemod(
      @SemgrepScan(ruleId = "sensitive-data-logging") final RuleSarif sarif,
      final OpenAIService openAI) {
    super(
        sarif,
        openAI,
        List.of(
            new NoActionLLMRemediationOutcome(
                "no_sensitive_data",
                "The tool's finding is a false positive. The code logs data that is not sensitive."),
            new CodeChangingLLMRemediationOutcome(
                "remove_sensitive_data",
                "The code logs sensitive data at INFO or higher levels.",
                "Remove the offending log statements.")),
        StandardModel.GPT_4O,
        StandardModel.GPT_4);
  }

  @Override
  protected String getThreatPrompt() {
    return """
      The tool has flagged a log statement that may be logging sensitive data.
      Examples of sensitive data include passwords, API keys, financial information such as bank account and credit card numbers, and personally identifiable information such as social security numbers.
      """;
  }
}
