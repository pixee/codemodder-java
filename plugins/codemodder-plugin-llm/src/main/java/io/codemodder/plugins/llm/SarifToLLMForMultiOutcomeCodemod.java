package io.codemodder.plugins.llm;

import static io.codemodder.plugins.llm.Tokens.countTokens;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An extension of {@link SarifPluginRawFileChanger} that uses large language models (LLMs) to
 * perform some analysis and categorize what's found to drive different potential code changes.
 *
 * <p>The inspiration for this type was the "remediate something found by tool X" use case. For
 * example, if a tool cites a vulnerability on a given line, we may want to take any of the
 * following actions:
 *
 * <ul>
 *   <li>Fix the identified issue by doing A
 *   <li>Fix the identified issue by doing B
 *   <li>Add a suppression comment to the given line since it's likely a false positive
 *   <li>Refactor the code so it doesn't trip the rule anymore, without actually "fixing it"
 *   <li>Do nothing, since the LLM can't determine which case the code is
 * </ul>
 *
 * <p>To accomplish that, we need the analysis to "bucket" the code into one of the above
 * categories.
 */
public abstract class SarifToLLMForMultiOutcomeCodemod extends SarifPluginRawFileChanger {

  private static final Logger logger =
      LoggerFactory.getLogger(SarifToLLMForMultiOutcomeCodemod.class);
  private final OpenAIService openAI;
  private final List<LLMRemediationOutcome> remediationOutcomes;

  protected SarifToLLMForMultiOutcomeCodemod(
      final RuleSarif sarif,
      final OpenAIService openAI,
      final List<LLMRemediationOutcome> remediationOutcomes) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
    this.remediationOutcomes = Objects.requireNonNull(remediationOutcomes);
    if (remediationOutcomes.size() < 2) {
      throw new IllegalArgumentException("must have 2+ remediation outcome");
    }
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
      FileDescription file = new FileDescription(context.path());

      boolean needDeeperInspectionAndPossibleChange = false;
      for (Result result : results) {
        CategorizeResponse analysis = categorize(file, result);
        String outcomeKey = analysis.getOutcomeKey();
        logger.debug("outcomeKey: {}", outcomeKey);
        logger.debug("analysis: {}", analysis.getAnalysis());
        if (outcomeKey == null || outcomeKey.isBlank()) {
          logger.debug("unable to determine outcome");
          continue;
        }
        Optional<LLMRemediationOutcome> outcome =
            remediationOutcomes.stream()
                .filter(oc -> oc.key().equals(analysis.outcomeKey))
                .findFirst();
        if (outcome.isEmpty()) {
          logger.debug("unable to find outcome for key: {}", analysis.outcomeKey);
          continue;
        }
        LLMRemediationOutcome matchedOutcome = outcome.get();
        logger.debug("outcomeKey: {}", matchedOutcome.key());
        logger.debug("description: {}", matchedOutcome.description());
        if (!matchedOutcome.shouldApplyCodeChanges()) {
          logger.debug("Matched outcome suggests there should be no code changes");
          continue;
        }
        needDeeperInspectionAndPossibleChange = true;
      }

      if (!needDeeperInspectionAndPossibleChange) {
        logger.debug("No need for deeper inspection and possible changes");
        return List.of();
      }

      CodeChangeResponse response = changeCode(file, results);
      logger.debug(
          "outcomes: {}",
          response.outcomes.stream().map(res -> res.outcomeKey).collect(Collectors.toList()));
      logger.debug("analysis: {}", response.codeChange);

      // If our second look determined that there are no outcomes associated with code changes, we
      // should quit
      if (response.outcomes.isEmpty()) {
        logger.debug("No outcomes detected");
        return List.of();
      }

      List<String> codeChangingOutcomeKeys =
          remediationOutcomes.stream()
              .filter(LLMRemediationOutcome::shouldApplyCodeChanges)
              .map(LLMRemediationOutcome::key)
              .toList();
      List<String> outcomes = response.outcomes.stream().map(res -> res.outcomeKey).toList();
      boolean anyRequireCodeChanges = outcomes.stream().anyMatch(codeChangingOutcomeKeys::contains);
      if (!anyRequireCodeChanges) {
        logger.debug("On second analysis, outcomes require no code changes");
        return List.of();
      }

      String codeChange = response.codeChange;
      // If the LLM was unable to fix the threat, don't change the file.
      if (codeChange == null || codeChange.length() == 0) {
        logger.info("unable to fix because diff not present: {}", context.path());
        return List.of();
      }

      // Apply the fix.
      List<String> fixedLines = LLMDiffs.applyDiff(file.getLines(), codeChange);

      // Ensure the end result isn't wonky.
      Patch<String> patch = DiffUtils.diff(file.getLines(), fixedLines);
      if (patch.getDeltas().size() == 0 || !isPatchExpected(patch)) {
        logger.error("unexpected or invalid patch: {}", patch);
        return List.of();
      }

      try {
        // Replace the file with the fixed version.
        String fixedFile = String.join(file.getLineSeparator(), fixedLines);
        Files.writeString(context.path(), fixedFile, file.getCharset());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      // Report all the changes at their respective line number
      return response.outcomes.stream()
          .filter(outcome -> outcome.fixDescription != null && !outcome.fixDescription.isBlank())
          .filter(outcome -> codeChangingOutcomeKeys.contains(outcome.outcomeKey))
          .map(outcome -> CodemodChange.from(outcome.line, outcome.fixDescription))
          .toList();
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
   * Returns whether the patch returned by the LLM is within the expectations of this codemod.
   *
   * @return {@code true} if the patch is expected; otherwise, {@code false}.
   */
  protected abstract boolean isPatchExpected(Patch<String> patch);

  private CategorizeResponse categorize(final FileDescription file, final Result result) {
    ChatMessage systemMessage = getSystemMessage();
    ChatMessage userMessage = getCategorizationUserMessage(file, result);

    int tokenCount = countTokens(List.of(systemMessage, userMessage));
    if (tokenCount > 3796) {
      // The max tokens for gpt-3.5-turbo-0613 is 4,096. If the estimated token count, which doesn't
      // include the function (~100 tokens) or the reply (~200 tokens), is close to the max, assume
      // the code is safe (for now).
      return new CategorizeResponse(
          "Ignoring file: estimated prompt token count (" + tokenCount + ") is too high.", null);
    } else {
      logger.debug("estimated prompt token count: {}", tokenCount);
    }

    return getCategorizationResponse(systemMessage, userMessage);
  }

  private CodeChangeResponse changeCode(final FileDescription file, final List<Result> results) {
    return getCodeChangeMResponse(getSystemMessage(), getChangeCodeMessage(file, results));
  }

  private CategorizeResponse getCategorizationResponse(
      final ChatMessage systemMessage, final ChatMessage userMessage) {
    // Create a function to get the LLM to return a structured response.
    ChatFunction function =
        ChatFunction.builder()
            .name("save_categorization_analysis")
            .description("Saves a categorization analysis.")
            .executor(
                CategorizeResponse.class,
                c -> c) // Return the `responseClass` instance when executed.
            .build();

    FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(function));

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("gpt-4-0613")
            .messages(List.of(systemMessage, userMessage))
            .functions(functionExecutor.getFunctions())
            .functionCall(ChatCompletionRequestFunctionCall.of(function.getName()))
            .temperature(0.2)
            .build();

    ChatCompletionResult result = openAI.createChatCompletion(request);
    logger.debug(result.getUsage().toString());

    ChatMessage response = result.getChoices().get(0).getMessage();
    return functionExecutor.execute(response.getFunctionCall());
  }

  private CodeChangeResponse getCodeChangeMResponse(
      final ChatMessage systemMessage, final ChatMessage userMessage) {
    // Create a function to get the LLM to return a structured response.
    ChatFunction function =
        ChatFunction.builder()
            .name("save_categorization_analysis_and_code_change")
            .description("Saves a categorization, analysis and code change.")
            .executor(CodeChangeResponse.class, c -> c)
            .build();

    FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(function));

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("gpt-4-0613")
            .messages(List.of(systemMessage, userMessage))
            .functions(functionExecutor.getFunctions())
            .functionCall(ChatCompletionRequestFunctionCall.of(function.getName()))
            .temperature(0.0)
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

  /** Analyze a single SARIF result and get feedback. */
  private ChatMessage getCategorizationUserMessage(
      final FileDescription file, final Result result) {
    Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
    int line = region.getStartLine();
    int column = region.getStartColumn();

    String outcomeDescriptions =
        remediationOutcomes.stream()
            .map(outcome -> "Outcome: '" + outcome.key() + "': " + outcome.description())
            .collect(Collectors.joining("\n"));

    return new ChatMessage(
        ChatMessageRole.SYSTEM.value(),
        CATEGORIZE_CODE_USER_MESSAGE_TEMPLATE
            .formatted(
                String.valueOf(line),
                String.valueOf(column),
                outcomeDescriptions,
                file.getFileName(),
                file.formatLinesWithLineNumbers())
            .strip());
  }

  /** Because the larger context size on GPT-4, we can ask it to handle all the results. */
  private ChatMessage getChangeCodeMessage(final FileDescription file, final List<Result> results) {

    String locations =
        results.stream()
            .map(r -> r.getLocations().get(0).getPhysicalLocation().getRegion())
            .map(r -> r.getStartLine() + ":" + r.getStartColumn())
            .collect(Collectors.joining(", "));
    return new ChatMessage(
        ChatMessageRole.USER.value(),
        CHANGE_CODE_USER_MESSAGE_TEMPLATE
            .formatted(locations, file.getFileName(), file.formatLinesWithLineNumbers())
            .strip());
  }

  private static final String SYSTEM_MESSAGE_TEMPLATE =
      """
            You are a security analyst bot. You are helping analyze Java code to assess its risk to a \
            specific security threat.
            %s
            """;

  private static final String CATEGORIZE_CODE_USER_MESSAGE_TEMPLATE =
      """
            A file with line numbers is provided below. Analyze ONLY line %s, column %s, and discern which "outcome" best describes the code. You should save your categorization analysis. Ignore any other file contents.
            Here are the possible outcomes:
            %s
            --- %s
            %s
            """;

  private static final String CHANGE_CODE_USER_MESSAGE_TEMPLATE =
      """
            A file with line numbers is provided below. Here are some results that were found in the file:
            %s
            For each result, decide which "outcome" you want to place it in. Then, if
            the category requires code change, make the changes as described in the "directions"
            to make the MINIMUM number of changes necessary to fix the threat:
            - Each change MUST be syntactically correct.
            - DO NOT change the file's formatting or comments.
            Create a diff patch for the changed file if and only if any of the outcomes require code changes.
            The patch must use the unified format with a header. Include \
            the diff patch and a summary of the changes with your analysis.
            Save your categorization and code change analysis.
            --- %s
            %s
            """;

  static class CategorizeResponse {
    @JsonPropertyDescription("A detailed analysis of how the analysis arrived at the outcome")
    @JsonProperty(required = true)
    private String analysis;

    @JsonPropertyDescription(
        "The category of the analysis, or empty if the analysis could not categorized")
    @JsonProperty(required = true)
    private String outcomeKey;

    @SuppressWarnings("unused") // needed by Jackson
    public CategorizeResponse() {}

    private CategorizeResponse(final String analysis, final String outcomeKey) {
      this.analysis = analysis;
      this.outcomeKey = outcomeKey;
    }

    public String getAnalysis() {
      return analysis;
    }

    public String getOutcomeKey() {
      return outcomeKey;
    }
  }

  static final class CodeChangeResponse {
    @JsonPropertyDescription(
        "The code change a diff patch in unified format. Required if any of the outcome keys indicate a change.")
    private String codeChange;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonPropertyDescription("The outcomes of the analysis of the locations given in the code.")
    private List<LocationOutcome> outcomes;
  }

  static final class LocationOutcome {
    @JsonPropertyDescription("The line to which this analysis is related")
    private int line;

    @JsonPropertyDescription("The column to which this analysis is related")
    private int column;

    @JsonPropertyDescription("The outcome key associated with this particular result location")
    private String outcomeKey;

    @JsonPropertyDescription(
        "A short description of the code change. Required only if the file needs a change.")
    private String fixDescription;
  }
}
