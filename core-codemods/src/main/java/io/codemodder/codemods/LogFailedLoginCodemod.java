package io.codemodder.codemods;

import static io.codemodder.CodemodResources.getClassResourceAsString;

import com.contrastsecurity.sarif.Result;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.InsertDelta;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.SarifToLLMForBinaryVerificationAndFixingCodemod;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/log-failed-login",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class LogFailedLoginCodemod extends SarifToLLMForBinaryVerificationAndFixingCodemod {

  @Inject
  public LogFailedLoginCodemod(
      @SemgrepScan(ruleId = "log-failed-login") final RuleSarif sarif, final OpenAIService openAI) {
    super(sarif, openAI);
  }

  @Override
  protected String getThreatPrompt(
      final CodemodInvocationContext context, final List<Result> results) {
    return getClassResourceAsString(getClass(), "threat_prompt.txt");
  }

  @Override
  protected String getFixPrompt() {
    return getClassResourceAsString(getClass(), "fix_prompt.txt");
  }

  @Override
  protected boolean isPatchExpected(Patch<String> patch) {
    // This codemod should make two or fewer modifications.
    if (patch.getDeltas().size() > 2) {
      return false;
    }

    // This codemod should only insert lines.
    for (AbstractDelta<String> delta : patch.getDeltas()) {
      if (!(delta instanceof InsertDelta<String>)) {
        return false;
      }
    }

    return true;
  }
}
