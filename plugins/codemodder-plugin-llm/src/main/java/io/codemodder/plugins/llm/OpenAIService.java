package io.codemodder.plugins.llm;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsJsonResponseFormat;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.util.HttpClientOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/** A custom service class to wrap the {@link OpenAIClient} */
public class OpenAIService {
  private final OpenAIClient api;
  private static final int TIMEOUT_SECONDS = 90;
  private final ModelMapper modelMapper;

  private static OpenAIClientBuilder builder(final KeyCredential key) {
    HttpClientOptions clientOptions = new HttpClientOptions();
    clientOptions.setReadTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));
    return new OpenAIClientBuilder()
        .retryPolicy(new RetryPolicy())
        .clientOptions(clientOptions)
        .credential(key);
  }

  OpenAIService(final ModelMapper mapper, final KeyCredential key) {
    this.modelMapper = mapper;
    this.api = builder(key).buildClient();
  }

  OpenAIService(final ModelMapper mapper, final KeyCredential key, final String endpoint) {
    this.modelMapper = mapper;
    this.api = builder(key).endpoint(endpoint).buildClient();
  }

  /**
   * Creates a new {@link OpenAIService} instance with the given OpenAI token.
   *
   * @param token the token to use
   * @return the new instance
   */
  public static OpenAIService fromOpenAI(final String token) {
    return new OpenAIService(
        new EnvironmentBasedModelMapper(), new KeyCredential(Objects.requireNonNull(token)));
  }

  /**
   * Creates a new {@link OpenAIService} instance with the given Azure OpenAI token and endpoint.
   *
   * @param token the token to use
   * @param endpoint the endpoint to use
   * @return the new instance
   */
  public static OpenAIService fromAzureOpenAI(final String token, final String endpoint) {
    return new OpenAIService(
        new EnvironmentBasedModelMapper(),
        new AzureKeyCredential(Objects.requireNonNull(token)),
        Objects.requireNonNull(endpoint));
  }

  /**
   * Gets the completion for the given messages.
   *
   * @param messages the messages
   * @param modelOrDeploymentName the model or deployment name
   * @return the completion
   */
  public String getJSONCompletion(
      final List<ChatRequestMessage> messages, final Model modelOrDeploymentName) {
    ChatCompletionsOptions options =
        new ChatCompletionsOptions(messages)
            .setTemperature(0D)
            .setN(1)
            .setResponseFormat(new ChatCompletionsJsonResponseFormat());
    final var modelName = modelMapper.getModelName(modelOrDeploymentName);
    ChatCompletions completions = this.api.getChatCompletions(modelName, options);
    return completions.getChoices().get(0).getMessage().getContent().trim();
  }

  /**
   * Returns an object of the given type based on the completion for the given messages.
   *
   * @param messages the messages
   * @param modelName the model name
   * @return the completion
   */
  public <T> T getResponseForPrompt(
      final List<ChatRequestMessage> messages, final Model modelName, final Class<T> responseType)
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
