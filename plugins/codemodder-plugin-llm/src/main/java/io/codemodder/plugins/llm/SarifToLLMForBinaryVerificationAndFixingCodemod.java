package io.codemodder.plugins.llm;

import static io.codemodder.plugins.llm.Tokens.countTokens;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginRawFileChanger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of {@link SarifPluginRawFileChanger} that uses large language models (LLMs) to more
 * deeply analyze and then fix the files found by the static analysis tool.
 *
 * <p>It has three phases:
 *
 * <ol>
 *   <li>Use a SARIF file to find locations of interest for analysis
 *   <li>Analyze the "threat" of the location found using a more inexpensive or faster model
 *   <li>Using a more reliable (and more expensive model), confirm the finding and rewrite the code
 * </ol>
 */
public abstract class SarifToLLMForBinaryVerificationAndFixingCodemod
    extends SarifPluginRawFileChanger {

  private static final Logger logger =
      LoggerFactory.getLogger(SarifToLLMForBinaryVerificationAndFixingCodemod.class);
  private final OpenAIService openAI;

  protected SarifToLLMForBinaryVerificationAndFixingCodemod(
      final RuleSarif sarif, final OpenAIService openAI) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
  }

  @Override
  public List<CodemodChange> onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    logger.debug("processing: {}", context.path());

    // For fine-tuning the semgrep rule, debug log the matching snippets in the file.
    results.forEach(
        (result) -> {
          Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
          logger.debug("{}:{}", region.getStartLine(), region.getSnippet().getText());
        });

    try {
      FileDescription file = new FileDescription(context.path());

      BinaryThreatAnalysis analysis = analyzeThreat(file, context, results);
      logger.debug("risk: {}", analysis.getRisk());
      logger.debug("analysis: {}", analysis.getAnalysis());

      if (analysis.getRisk() == BinaryThreatRisk.LOW) {
        return List.of();
      }

      BinaryThreatAnalysisAndFix fix = fixThreat(file, context, results);
      logger.debug("risk: {}", fix.getRisk());
      logger.debug("analysis: {}", fix.getAnalysis());
      logger.debug("fix: {}", fix.getFix());
      logger.debug("fix description: {}", fix.getFixDescription());

      // If our second look determined that the risk of the threat is low, don't change the file.
      if (fix.getRisk() == BinaryThreatRisk.LOW) {
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

      // Report all the changes at the line number of the first change.
      int line = patch.getDeltas().get(0).getSource().getPosition() + 1; // Position is 0-based.
      return List.of(CodemodChange.from(line, fix.getFixDescription()));
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
  protected abstract String getThreatPrompt(CodemodInvocationContext context, List<Result> results);

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

  private BinaryThreatAnalysis analyzeThreat(
      final FileDescription file,
      final CodemodInvocationContext context,
      final List<Result> results) {
    ChatMessage systemMessage = getSystemMessage(context, results);
    ChatMessage userMessage = getAnalyzeUserMessage(file);

    int tokenCount = countTokens(List.of(systemMessage, userMessage));
    if (tokenCount > 3796) {
      // The max tokens for gpt-3.5-turbo-0613 is 4,096. If the estimated token count, which doesn't
      // include the function (~100 tokens) or the reply (~200 tokens), is close to the max, assume
      // the code is safe (for now).
      return new BinaryThreatAnalysis(
          "Ignoring file: estimated prompt token count (" + tokenCount + ") is too high.",
          BinaryThreatRisk.LOW);
    } else {
      logger.debug("estimated prompt token count: {}", tokenCount);
    }

    return getLLMResponse(
        "gpt-3.5-turbo-0613", 0.2D, systemMessage, userMessage, BinaryThreatAnalysis.class);
  }

  private BinaryThreatAnalysisAndFix fixThreat(
      final FileDescription file,
      final CodemodInvocationContext context,
      final List<Result> results) {
    return getLLMResponse(
        "gpt-4-0613",
        0D,
        getSystemMessage(context, results),
        getFixUserMessage(file),
        BinaryThreatAnalysisAndFix.class);
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

  private ChatMessage getSystemMessage(CodemodInvocationContext context, List<Result> results) {
    String threatPrompt = getThreatPrompt(context, results);
    return new ChatMessage(
        ChatMessageRole.SYSTEM.value(),
        SYSTEM_MESSAGE_TEMPLATE.formatted(threatPrompt.strip()).strip());
  }

  private ChatMessage getAnalyzeUserMessage(final FileDescription file) {
    return new ChatMessage(
        ChatMessageRole.SYSTEM.value(),
        ANALYZE_USER_MESSAGE_TEMPLATE
            .formatted(file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
  }

  private ChatMessage getFixUserMessage(final FileDescription file) {
    return new ChatMessage(
        ChatMessageRole.USER.value(),
        FIX_USER_MESSAGE_TEMPLATE
            .formatted(
                getFixPrompt().strip(), file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
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
}
