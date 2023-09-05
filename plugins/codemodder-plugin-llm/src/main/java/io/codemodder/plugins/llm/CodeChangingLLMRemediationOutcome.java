package io.codemodder.plugins.llm;

import java.util.Objects;

/** Models the parameters for a remediation analysis + actual direction for changing the code. */
public record CodeChangingLLMRemediationOutcome(String key, String description, String fix)
    implements LLMRemediationOutcome {

  public CodeChangingLLMRemediationOutcome {
    Objects.requireNonNull(key);
    Objects.requireNonNull(description);
    Objects.requireNonNull(fix);
  }

  @Override
  public boolean shouldApplyCodeChanges() {
    return true;
  }
}
