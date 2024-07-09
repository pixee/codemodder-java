package io.codemodder.plugins.llm;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsJsonResponseFormat;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.HttpClientOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/** A custom service class to wrap the {@link OpenAIClient} */
public class OpenAIService {
  private final OpenAIClient api;
  private static final int TIMEOUT_SECONDS = 90;

  public OpenAIService(final String token) {
    HttpClientOptions clientOptions = new HttpClientOptions();
    clientOptions.setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    final var builder =
        new OpenAIClientBuilder().retryPolicy(new RetryPolicy()).clientOptions(clientOptions);
    this.api = builder.buildClient();
  }

  public String getJSONCompletion(
      final List<ChatRequestMessage> messages, final String modelOrDeploymentName) {
    ChatCompletionsOptions options =
        new ChatCompletionsOptions(messages)
            .setTemperature(0D)
            .setN(1)
            .setResponseFormat(new ChatCompletionsJsonResponseFormat());
    ChatCompletions completions = this.api.getChatCompletions(modelOrDeploymentName, options);
    return completions.getChoices().get(0).getMessage().getContent().trim();
  }

  public <T> T getResponseForPrompt(
      final List<ChatRequestMessage> messages, final String modelName, final Class<T> responseType)
      throws IOException {
    String json = getJSONCompletion(messages, modelName);

    // we've seen this with turbo/lesser models
    if (json.startsWith("```json") && json.endsWith("```")) {
      json = json.substring(7, json.length() - 3);
    }

    // try to deserialize the content as is
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(json, responseType);
    } catch (IOException e) {
      int firstBorder = json.indexOf("```json");
      int lastBorder = json.lastIndexOf("```");
      if (firstBorder != -1 && lastBorder != -1 && lastBorder > firstBorder) {
        json = json.substring(firstBorder + 7, lastBorder);
      }
      return mapper.readValue(json, responseType);
    }
  }
}
