package io.codemodder.plugins.llm;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.EncodingDetector;
import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginRawFileChanger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of {@link SarifPluginRawFileChanger} that uses large language models (LLMs) to
 * analyze and fix the files found by the static analysis tool.
 */
public abstract class LLMAssistedCodemod extends SarifPluginRawFileChanger {

  private static final Logger logger = LoggerFactory.getLogger(LLMAssistedCodemod.class);
  private final OpenAIService openAI;

  protected LLMAssistedCodemod(final RuleSarif sarif, final OpenAIService openAI) {
    super(sarif);
    this.openAI = openAI;
  }

  @Override
  public List<CodemodChange> onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    logger.info("processing: {}", context.path());

    // For fine-tuning the semgrep rule, debug log the matching snippets in the file.
    results.forEach(
        (result) -> {
          Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
          logger.debug("{}:{}", region.getStartLine(), region.getSnippet().getText());
        });

    try {
      CodemodInvocationContextFile file = new CodemodInvocationContextFile(context.path());

      ThreatAnalysis analysis = analyzeThreat(file);
      logger.debug("risk: {}", analysis.getRisk());
      logger.debug("analysis: {}", analysis.getAnalysis());

      if (analysis.getRisk() == Risk.LOW) {
        return List.of();
      }

      ThreatFix fix = fixThreat(file);
      logger.debug("risk: {}", fix.getRisk());
      logger.debug("analysis: {}", fix.getAnalysis());
      logger.debug("fix: {}", fix.getFix());
      logger.debug("fix description: {}", fix.getFixDescription());

      // If our second look determined that the risk of the threat is low, don't change the file.
      if (fix.getRisk() == Risk.LOW) {
        return List.of();
      }

      // If the LLM was unable to fix the threat, don't change the file.
      if (fix.getFix() == null || fix.getFix().length() == 0) {
        logger.info("unable to fix: {}", context.path());
        return List.of();
      }

      // Apply the fix.
      List<String> fixedLines = LLMDiffs.applyDiff(file.getLines(), fix.getFix());

      // Ensure the end result isn't wonky.
      Patch<String> patch = DiffUtils.diff(file.getLines(), fixedLines);
      if (patch.getDeltas().size() == 0 || !isPatchExpected(patch)) {
        logger.error("unexpected patch: {}", patch);
        return List.of();
      }

      try {
        // Replace the file with the fixed version.
        String fixedFile = String.join(file.getLineSeparator(), fixedLines);
        Files.writeString(context.path(), fixedFile, file.getCharset());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      return List.of(
          CodemodChange.from(
              patch.getDeltas().get(0).getSource().getPosition(), fix.getFixDescription()));
    } catch (Exception e) {
      logger.error("failed to process: {}", context.path(), e);
      throw e;
    }
  }

  /**
   * Instructs the LLM on how to assess the risk of the threat.
   *
   * @return The prompt.
   */
  protected abstract String getThreatPrompt();

  /**
   * Instructs the LLM on how to fix the threat.
   *
   * @return The prompt.
   */
  protected abstract String getFixPrompt();

  /**
   * Returns whether the patch returned by the LLM is within the expectations of this codemod.
   *
   * @return {@code true} if the patch is expected; otherwise, {@code false}.
   */
  protected abstract boolean isPatchExpected(Patch<String> patch);

  /**
   * Returns a class resource as a {@code String}.
   *
   * <p>The absolute name of the class resource is of the following form:
   *
   * <blockquote>
   *
   * {@code /modifiedPackageName/className/relativeName}
   *
   * </blockquote>
   *
   * Where the {@code modifiedPackageName} is the package name of this object with {@code '/'}
   * substituted for {@code '.'}.
   *
   * @param relativeName The relative name of the resource.
   * @return The resource as a {@code String}.
   * @throws MissingResourceException If the resource was not found.
   */
  protected String getClassResourceAsString(final String relativeName) {
    String resourceName = "/" + getClass().getName().replace('.', '/') + "/" + relativeName;
    try (InputStream stream = getClass().getResourceAsStream(resourceName)) {
      if (stream == null) {
        throw new MissingResourceException(resourceName, getClass().getName(), resourceName);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private ThreatAnalysis analyzeThreat(final CodemodInvocationContextFile file) {
    ChatMessage systemMessage = getSystemMessage();
    ChatMessage userMessage = getAnalyzeUserMessage(file);

    int tokenCount = countTokens(List.of(systemMessage, userMessage));
    if (tokenCount > 3796) {
      // The max tokens for gpt-3.5-turbo-0613 is 4,096. If the estimated token count, which doesn't
      // include the function (~100 tokens) or the reply (~200 tokens), is close to the max, assume
      // the code is safe (for now).
      return new ThreatAnalysis(
          "Ignoring file: estimated prompt token count (" + tokenCount + ") is too high.",
          Risk.LOW);
    } else {
      logger.debug("estimated prompt token count: {}", tokenCount);
    }

    return getLLMResponse(
        "gpt-3.5-turbo-0613", 0.2D, systemMessage, userMessage, ThreatAnalysis.class);
  }

  private ThreatFix fixThreat(final CodemodInvocationContextFile file) {
    return getLLMResponse(
        "gpt-4-0613", 0D, getSystemMessage(), getFixUserMessage(file), ThreatFix.class);
  }

  private <T> T getLLMResponse(
      final String model,
      final Double temperature,
      final ChatMessage systemMessage,
      final ChatMessage userMessage,
      final Class<T> responseClass) {
    // Create a function to get the LLM to return a structured response.
    ChatFunction function =
        ChatFunction.builder()
            .name("save_analysis")
            .description("Saves a security threat analysis.")
            .executor(responseClass, c -> c) // Return the `responseClass` instance when executed.
            .build();

    FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(function));

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model(model)
            .messages(List.of(systemMessage, userMessage))
            .functions(functionExecutor.getFunctions())
            .functionCall(ChatCompletionRequestFunctionCall.of(function.getName()))
            .temperature(temperature)
            .build();

    ChatCompletionResult result = openAI.createChatCompletion(request);
    logger.debug(result.getUsage().toString());

    ChatMessage response = result.getChoices().get(0).getMessage();
    return functionExecutor.execute(response.getFunctionCall());
  }

  private ChatMessage getSystemMessage() {
    return new ChatMessage(
        ChatMessageRole.SYSTEM.value(),
        SYSTEM_MESSAGE_TEMPLATE.formatted(getThreatPrompt().strip()).strip());
  }

  private ChatMessage getAnalyzeUserMessage(final CodemodInvocationContextFile file) {
    return new ChatMessage(
        ChatMessageRole.SYSTEM.value(),
        ANALYZE_USER_MESSAGE_TEMPLATE
            .formatted(file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
  }

  private ChatMessage getFixUserMessage(final CodemodInvocationContextFile file) {
    return new ChatMessage(
        ChatMessageRole.USER.value(),
        FIX_USER_MESSAGE_TEMPLATE
            .formatted(
                getFixPrompt().strip(), file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
  }

  /**
   * Estimates the number of tokens the messages will consume when passed to the {@code
   * gpt-3.5-turbo-0613} or {@code gpt-4-0613} models.
   *
   * <p>This does not yet support estimating the number of tokens the functions will consume, since
   * the <a
   * href="https://community.openai.com/t/how-to-calculate-the-tokens-when-using-function-call/266573/1">unofficial
   * solutions</a> are brittle.
   *
   * <p>We should be able to replace this with {@code TikTokensUtil.tokens} when the <a
   * href="https://github.com/TheoKanning/openai-java/pull/311">feature</a> is released.
   *
   * @param messages The messages.
   * @return The number of tokens.
   * @see <a
   *     href="https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb">How
   *     to count tokens with tiktoken</a>
   */
  private int countTokens(final List<ChatMessage> messages) {
    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    int count = 0;
    for (ChatMessage message : messages) {
      count += 3; // Both gpt-3.5-turbo-0613 and gpt-4-0613 consume 3 tokens per message.
      count += encoding.countTokens(message.getContent());
      count += encoding.countTokens(message.getRole());
    }
    count += 3; // Every reply is primed with <|start|>assistant<|message|>.

    return count;
  }

  private static final String SYSTEM_MESSAGE_TEMPLATE =
      """
      You are a security analyst bot. You are helping analyze Java code to assess its risk to a \
      specific security threat.

      %s
      """;

  private static final String ANALYZE_USER_MESSAGE_TEMPLATE =
      """
      A file with line numbers is provided below. Analyze it and save your threat analysis.

      --- %s
      %s
      """;

  private static final String FIX_USER_MESSAGE_TEMPLATE =
      """
      A file with line numbers is provided below. Analyze it. If the risk is HIGH, use these rules \
      to make the MINIMUM number of changes necessary to reduce the file's risk to LOW:
      - Each change MUST be syntactically correct.
      - DO NOT change the file's formatting or comments.
      %s

      Create a diff patch for the changed file, using the unified format with a header. Include \
      the diff patch and a summary of the changes with your threat analysis.

      Save your threat analysis.

      --- %s
      %s
      """;

  /**
   * Provides some methods to simplify working with the file at {@link
   * CodemodInvocationContext#path()}.
   */
  static final class CodemodInvocationContextFile {
    private final String fileName;
    private final Charset charset;

    private final String lineSeparator;
    private final List<String> lines;

    public CodemodInvocationContextFile(final Path path) {
      fileName = path.getFileName().toString();

      try {
        charset = Charset.forName(EncodingDetector.create().detect(path).orElse("UTF-8"));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      try {
        String s = Files.readString(path, charset);
        lineSeparator = detectLineSeparator(s);
        lines = List.of(s.split("\\R", -1));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    public String getFileName() {
      return fileName;
    }

    public Charset getCharset() {
      return charset;
    }

    public String getLineSeparator() {
      return lineSeparator;
    }

    public List<String> getLines() {
      return lines;
    }

    public String formatLinesWithLineNumbers() {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < lines.size(); i++) {
        sb.append(i + 1);
        sb.append(": ");
        sb.append(lines.get(i));
        sb.append("\n");
      }
      return sb.toString();
    }

    private String detectLineSeparator(final String s) {
      Matcher m = Pattern.compile("(\\R)").matcher(s);
      if (m.find()) {
        // This assumes that the first line separator found is the one to use.
        return m.group(1);
      }
      return "\n";
    }
  }

  enum Risk {
    HIGH,
    LOW;
  }

  static class ThreatAnalysis {
    @JsonPropertyDescription("A detailed analysis of how the risk was assessed.")
    @JsonProperty(required = true)
    private String analysis;

    @JsonPropertyDescription("The risk of the security threat, either HIGH or LOW.")
    @JsonProperty(required = true)
    private Risk risk;

    public ThreatAnalysis() {}

    public ThreatAnalysis(final String analysis, final Risk risk) {
      this.analysis = analysis;
      this.risk = risk;
    }

    public String getAnalysis() {
      return analysis;
    }

    public Risk getRisk() {
      return risk;
    }
  }

  static final class ThreatFix extends ThreatAnalysis {
    @JsonPropertyDescription(
        "The fix as a diff patch in unified format. Required if the risk is HIGH.")
    private String fix;

    @JsonPropertyDescription("A short description of the fix. Required if the file is fixed.")
    private String fixDescription;

    public String getFix() {
      return fix;
    }

    public String getFixDescription() {
      return fixDescription;
    }
  }
}
