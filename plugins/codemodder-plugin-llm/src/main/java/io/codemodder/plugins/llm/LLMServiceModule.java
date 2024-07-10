package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;

/** Provides configured LLM services. */
public final class LLMServiceModule extends AbstractModule {

  private static final String OPENAI_KEY_NAME = "CODEMODDER_OPENAI_API_KEY";
  private static final String AZURE_OPENAI_KEY_NAME = "CODEMODDER_AZURE_OPENAI_API_KEY";
  private static final String AZURE_OPENAI_ENDPOINT = "CODEMODDER_AZURE_OPENAI_ENDPOINT";

  @Override
  protected void configure() {
    final var azureOpenAIKey = System.getenv(AZURE_OPENAI_KEY_NAME);
    final var azureOpenAIEndpoint = System.getenv(AZURE_OPENAI_ENDPOINT);
    if ((azureOpenAIEndpoint == null) != (azureOpenAIKey == null)) {
      throw new IllegalArgumentException(
          "Both or neither of "
              + AZURE_OPENAI_KEY_NAME
              + " and "
              + AZURE_OPENAI_ENDPOINT
              + " must be set");
    }
    if (azureOpenAIKey != null) {
      bind(OpenAIService.class)
          .toProvider(() -> OpenAIService.fromAzureOpenAI(azureOpenAIKey, azureOpenAIEndpoint));
      return;
    }

    bind(OpenAIService.class).toProvider(() -> OpenAIService.fromOpenAI(getOpenAIToken()));
  }

  private String getOpenAIToken() {
    final var openAIKey = System.getenv(OPENAI_KEY_NAME);
    if (openAIKey == null) {
      throw new IllegalArgumentException(OPENAI_KEY_NAME + " environment variable must be set");
    }
    return openAIKey;
  }
}
