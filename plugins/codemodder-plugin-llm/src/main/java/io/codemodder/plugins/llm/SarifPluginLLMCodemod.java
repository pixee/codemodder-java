package io.codemodder.plugins.llm;

import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginRawFileChanger;
import java.util.Objects;

public abstract class SarifPluginLLMCodemod extends SarifPluginRawFileChanger {
  protected final OpenAIService openAI;

  public SarifPluginLLMCodemod(RuleSarif sarif, final OpenAIService openAI) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
  }

  @Override
  public boolean shouldRun() {
    return openAI.isServiceAvailable();
  }
}
