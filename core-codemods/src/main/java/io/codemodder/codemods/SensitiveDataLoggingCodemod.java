package io.codemodder.codemods;

import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeleteDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.Codemod;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.plugins.llm.LLMAssistedCodemod;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/** A codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SensitiveDataLoggingCodemod extends LLMAssistedCodemod {

  @Inject
  public SensitiveDataLoggingCodemod(
      @SemgrepScan(ruleId = "sensitive-data-logging") final RuleSarif sarif,
      final OpenAIService openAI) {
    super(sarif, openAI);
  }

  @Override
  protected String getThreatPrompt() {
    return getClassResourceAsString("threat_prompt.txt");
  }

  @Override
  protected String getFixPrompt() {
    return getClassResourceAsString("fix_prompt.txt");
  }

  @Override
  protected boolean isPatchExpected(Patch<String> patch) {
    // This codemod should only delete lines.
    for (AbstractDelta<String> delta : patch.getDeltas()) {
      if (!(delta instanceof DeleteDelta<String>)) {
        return false;
      }
    }

    return true;
  }
}
