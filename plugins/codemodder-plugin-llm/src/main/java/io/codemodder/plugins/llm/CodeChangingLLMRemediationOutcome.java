package io.codemodder.plugins.llm;

import java.util.Objects;

/** Models the parameters for a remediation analysis + actual direction for changing the code. */
public record CodeChangingLLMRemediationOutcome(String key, String description, String fixPrompt)
    implements LLMRemediationOutcome {

  public CodeChangingLLMRemediationOutcome {
    Objects.requireNonNull(key);
    Objects.requireNonNull(description);
    Objects.requireNonNull(fixPrompt);
  }

  @Override
  public boolean shouldApplyCodeChanges() {
    return true;
  }
}
