package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;

/** Provides configured LLM services. */
public final class LLMServiceModule extends AbstractModule {

  private static final String TOKEN_NAME = "CODEMODDER_OPENAI_API_KEY";

  @Override
  protected void configure() {
    bind(OpenAIService.class).toProvider(() -> new OpenAIService(getToken()));
  }

  private String getToken() {
    String token = System.getenv(TOKEN_NAME);
    if (token == null) {
      throw new IllegalArgumentException(TOKEN_NAME + " environment variable must be set");
    }
    return token;
  }
}
