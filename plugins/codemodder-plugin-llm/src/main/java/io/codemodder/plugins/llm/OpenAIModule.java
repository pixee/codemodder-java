package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;
import com.theokanning.openai.service.OpenAiService;
import io.codemodder.CodeChanger;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** Provides the OpenAI service to the codemodder plugin. */
public final class OpenAIModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path codeDirectory;

  public OpenAIModule(
      final Path codeDirectory, final List<Class<? extends CodeChanger>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
  }

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
