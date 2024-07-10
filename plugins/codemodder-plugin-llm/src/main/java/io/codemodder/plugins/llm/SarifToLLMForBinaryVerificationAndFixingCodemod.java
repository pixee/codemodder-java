package io.codemodder.plugins.llm;

import static io.codemodder.plugins.llm.StandardModel.GPT_3_5_TURBO_0125;

import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
    this(sarif, openAI, GPT_3_5_TURBO_0125);
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
    } catch (IOException e) {
      logger.error("failed to process: {}", context.path(), e);
      throw new UncheckedIOException(e);
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
      final List<Result> results)
      throws IOException {
    final ChatRequestSystemMessage systemMessage = getSystemMessage(context, results);
    final ChatRequestUserMessage userMessage = getAnalyzeUserMessage(file);

    // If the estimated token count, which doesn't include the function (~100 tokens) or the reply
    // (~200 tokens), is close to the max, then assume the code is safe (for now).
    int tokenCount =
        model.tokens(List.of(systemMessage.getContent(), userMessage.getContent().toString()));
    if (tokenCount > model.contextWindow() - 300) {
      return new BinaryThreatAnalysis(
          "Ignoring file: estimated prompt token count (" + tokenCount + ") is too high.",
          BinaryThreatRisk.LOW);
    } else {
      logger.debug("estimated prompt token count: {}", tokenCount);
    }

    return openAI.getResponseForPrompt(
        List.of(systemMessage, userMessage), model, BinaryThreatAnalysis.class);
  }

  private BinaryThreatAnalysisAndFix fixThreat(
      final FileDescription file,
      final CodemodInvocationContext context,
      final List<Result> results)
      throws IOException {
    return openAI.getResponseForPrompt(
        List.of(getSystemMessage(context, results), getFixUserMessage(file)),
        model,
        BinaryThreatAnalysisAndFix.class);
  }

  private ChatRequestSystemMessage getSystemMessage(
      CodemodInvocationContext context, List<Result> results) {
    String threatPrompt = getThreatPrompt(context, results);
    return new ChatRequestSystemMessage(
        SYSTEM_MESSAGE_TEMPLATE.formatted(threatPrompt.strip()).strip());
  }

  private ChatRequestUserMessage getAnalyzeUserMessage(final FileDescription file) {
    return new ChatRequestUserMessage(
        ANALYZE_USER_MESSAGE_TEMPLATE
            .formatted(file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
  }

  private ChatRequestUserMessage getFixUserMessage(final FileDescription file) {
    return new ChatRequestUserMessage(
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

      Return a JSON object with the following properties in this order:
        - `analysis`: A detailed analysis of how the risk was assessed.
        - `risk`: The risk of the security threat, either HIGH or LOW.
      --- %s
      %s
      """;

  private static final String FIX_USER_MESSAGE_TEMPLATE =
      """
      A file with line numbers is provided below. Analyze it. If the risk is HIGH, use these rules to make the MINIMUM number of changes necessary to reduce the file's risk to LOW:
      - Each change MUST be syntactically correct.
      %s

      Any code changes to reduce the file's risk to LOW must be stored in a diff patch format. Follow these instructions when creating the patch:
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
       for (var i = 0; i < array.length; i++) {
         This line is unchanged.
      -  This is the original line
      +  This is the replacement line
       }
       Here is another unchanged line.
      @@ ... @@
      -This line has been removed but not replaced.
       This line is unchanged.
      ```

      Now save your threat analysis.

      Return a JSON object with the following properties in this order:
        - `analysis`: A detailed analysis of how the risk was assessed.
        - `risk`: The risk of the security threat, either HIGH or LOW.
        - `fixDescription`: A short description of the fix. Required if the file is fixed.
        - `fix`: The fix as a diff patch in unified format. Required if the risk is HIGH.
      --- %s
      %s
      """;

  private static final Logger logger =
      LoggerFactory.getLogger(SarifToLLMForBinaryVerificationAndFixingCodemod.class);
}
