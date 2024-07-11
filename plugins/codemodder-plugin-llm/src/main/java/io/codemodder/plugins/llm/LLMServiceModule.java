package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides configured LLM services. */
public final class LLMServiceModule extends AbstractModule {

  private static final String OPENAI_KEY_NAME = "CODEMODDER_OPENAI_API_KEY";
  private static final String AZURE_OPENAI_KEY_NAME = "CODEMODDER_AZURE_OPENAI_API_KEY";
  private static final String AZURE_OPENAI_ENDPOINT = "CODEMODDER_AZURE_OPENAI_ENDPOINT";
  private static final Logger logger = LoggerFactory.getLogger(LLMServiceModule.class);

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
      logger.info("Using Azure OpenAI service with endpoint {}", azureOpenAIEndpoint);
      bind(OpenAIService.class)
          .toProvider(() -> OpenAIService.fromAzureOpenAI(azureOpenAIKey, azureOpenAIEndpoint));
      return;
    }

    final var openAIKey = System.getenv(OPENAI_KEY_NAME);
    if (openAIKey != null) {
      logger.info("Using OpenAI service");
      bind(OpenAIService.class).toProvider(() -> OpenAIService.fromOpenAI(openAIKey));
      return;
    }

    logger.info("No LLM service available");
    bind(OpenAIService.class).toProvider(OpenAIService::noServiceAvailable);
  }
}
