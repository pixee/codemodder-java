package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;

/** Provides configured LLM services. */
public final class LLMServiceModule extends AbstractModule {

  private static final String TOKEN_NAME = "CODEMODDER_OPENAI_API_KEY";

  @Override
  protected void configure() {
    bind(OpenAIService.class).toProvider(() -> new OpenAIService(getToken()));
    bind(OpenAiService.class)
        .toProvider(() -> new OpenAiService(getToken(), Duration.ofSeconds(60)));
  }

  private String getToken() {
    String token = System.getenv(TOKEN_NAME);
    if (token == null) {
      throw new IllegalArgumentException(TOKEN_NAME + " environment variable must be set");
    }
    return token;
  }
}
