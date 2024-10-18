package io.codemodder.plugins.llm;

import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginRawFileChanger;
import java.util.Objects;

/** A base class for LLM codemods that process SARIF and use the OpenAI service. */
public abstract class SarifPluginLLMCodemod extends SarifPluginRawFileChanger {

  protected final OpenAIService openAI;

  protected SarifPluginLLMCodemod(RuleSarif sarif, final OpenAIService openAI) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
  }

  /**
   * Indicates whether the codemod should run.
   *
   * <p>Subclasses can override this method to add additional hecks but should call
   * super.shouldRun() to ensure the OpenAI service is available.
   */
  @Override
  public boolean shouldRun() {
    return openAI.isServiceAvailable();
  }
}
