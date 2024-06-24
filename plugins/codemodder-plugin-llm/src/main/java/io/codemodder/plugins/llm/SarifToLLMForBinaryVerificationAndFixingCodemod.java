package io.codemodder.plugins.llm;

import static io.codemodder.plugins.llm.StandardModel.GPT_3_5_TURBO;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.*;
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

  private final OpenAIService openAI;
  private final Model model;

  protected SarifToLLMForBinaryVerificationAndFixingCodemod(
      final RuleSarif sarif, final OpenAIService openAI, final Model model) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
    this.model = Objects.requireNonNull(model);
  }

  /**
   * For backwards compatibility with a previous version of this API, uses a GPT 3.5 Turbo model.
   */
  protected SarifToLLMForBinaryVerificationAndFixingCodemod(
      final RuleSarif sarif, final OpenAIService openAI) {
    this(sarif, openAI, GPT_3_5_TURBO);
  }

  @Override
  public CodemodFileScanningResult onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    logger.debug("processing: {}", context.path());

    // For fine-tuning the semgrep rule, debug log the matching snippets in the file.
    results.forEach(
        result -> {
          Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
          logger.debug("{}:{}", region.getStartLine(), region.getSnippet().getText());
        });

    try {
      FileDescription file = FileDescription.from(context.path());

      BinaryThreatAnalysis analysis = analyzeThreat(file, context, results);
      logger.debug("risk: {}", analysis.getRisk());
      logger.debug("analysis: {}", analysis.getAnalysis());

      if (analysis.getRisk() == BinaryThreatRisk.LOW) {
        return CodemodFileScanningResult.none();
      }

      BinaryThreatAnalysisAndFix fix = fixThreat(file, context, results);
      logger.debug("{}", fix);

      // If our second look determined that the risk of the threat is low, don't change the file.
      if (fix.getRisk() == BinaryThreatRisk.LOW) {
        return CodemodFileScanningResult.none();
      }

      // If the LLM was unable to fix the threat, don't change the file.
      if (fix.getFix() == null || fix.getFix().isEmpty()) {
        logger.info("unable to fix: {}", context.path());
        return CodemodFileScanningResult.none();
      }

      // Apply the fix.
      List<String> fixedLines = LLMDiffs.applyDiff(file.getLines(), fix.getFix());

      // Ensure the end result isn't wonky.
      Patch<String> patch = DiffUtils.diff(file.getLines(), fixedLines);
      if (patch.getDeltas().isEmpty() || !isPatchExpected(patch)) {
        logger.error("unexpected patch: {}", patch);
        return CodemodFileScanningResult.none();
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
      List<CodemodChange> changes = List.of(CodemodChange.from(line, fix.getFixDescription()));
      return CodemodFileScanningResult.withOnlyChanges(changes);
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

    // If the estimated token count, which doesn't include the function (~100 tokens) or the reply
    // (~200 tokens), is close to the max, then assume the code is safe (for now).
    int tokenCount = model.tokens(List.of(systemMessage, userMessage));
    if (tokenCount > model.contextWindow() - 300) {
      return new BinaryThreatAnalysis(
          "Ignoring file: estimated prompt token count (" + tokenCount + ") is too high.",
          BinaryThreatRisk.LOW);
    } else {
      logger.debug("estimated prompt token count: {}", tokenCount);
    }

    return getLLMResponse(model.id(), 0.2D, systemMessage, userMessage, BinaryThreatAnalysis.class);
  }

  private BinaryThreatAnalysisAndFix fixThreat(
      final FileDescription file,
      final CodemodInvocationContext context,
      final List<Result> results) {
    return getLLMResponse(
        model.id(),
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
    logger.debug("{}", result.getUsage());

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
      %s

      Create a diff patch for the changed file. Follow these instructions when creating the patch:
        - Your output must be in the form a unified diff patch that will be applied by your coworkers.
        - The output must be similar to the output of `diff -U0`. Do not include line number ranges.
        - Start each hunk of changes with a `@@ ... @@` line.
        - Each change in a file should be a separate hunk in the diff.
        - It is very important for the change to contain only what is minimally required to fix the problem.
        - Remember that whitespace and indentation changes can be important. Preserve the original formatting and indentation. Do not replace tabs with spaces or vice versa. If the original code uses tabs, use tabs in the patch. Encode tabs using a tab literal (\\\\t). If the original code uses spaces, use spaces in the patch. Do not add spaces where none were present in the original code. **THIS IS ESPECIALLY IMPORTANT AT THE BEGINNING OF DIFF LINES.**
        - The unified diff must be accurate and complete.
        - The unified diff will be applied to the source code by your coworkers.

      Here's an example of a unified diff:
      ```diff
      --- a/file.txt
      +++ b/file.txt
      @@ ... @@
       This line is unchanged.
      -This is the original line
      +This is the new line
       Here is another unchanged line.
      @@ ... @@
      -This line has been removed and not replaced.
       This line is unchanged.

      ```
      Now save your threat analysis.

      --- %s
      %s
      """;

  private static final Logger logger =
      LoggerFactory.getLogger(SarifToLLMForBinaryVerificationAndFixingCodemod.class);
}
