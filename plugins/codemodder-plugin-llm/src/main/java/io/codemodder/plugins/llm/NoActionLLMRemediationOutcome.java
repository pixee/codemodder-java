package io.codemodder.plugins.llm;

import java.util.Objects;

/** Models the parameters for a remediation analysis that results in no code changes. */
public record NoActionLLMRemediationOutcome(String key, String description)
    implements LLMRemediationOutcome {

  public NoActionLLMRemediationOutcome {
    Objects.requireNonNull(key);
    Objects.requireNonNull(description);
  }

  @Override
  public String fix() {
    return "N/A";
  }

  @Override
  public boolean shouldApplyCodeChanges() {
    return false;
  }
}
