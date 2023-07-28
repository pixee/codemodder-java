package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;

/** Provides the OpenAI service to the codemodder plugin. */
public final class OpenAIModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(OpenAiService.class)
        .toProvider(
            () -> {
              // prefer the environment
              String apiKey = System.getenv("CODEMODDER_OPENAI_API_KEY");
              if (apiKey == null) {
                throw new IllegalArgumentException(
                    "CODEMODDER_OPENAI_API_KEY environment variable must be set");
              }
              return new OpenAiService(apiKey, Duration.ofSeconds(60));
            });
  }
}
