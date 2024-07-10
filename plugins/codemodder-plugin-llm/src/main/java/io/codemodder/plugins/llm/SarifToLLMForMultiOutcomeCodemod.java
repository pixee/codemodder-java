package io.codemodder.plugins.llm;

import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import io.codemodder.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
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
  private final Model categorizationModel;
  private final Model codeChangingModel;

  protected SarifToLLMForMultiOutcomeCodemod(
      final RuleSarif sarif,
      final OpenAIService openAI,
      final List<LLMRemediationOutcome> remediationOutcomes) {
    this(
        sarif,
        openAI,
        remediationOutcomes,
        StandardModel.GPT_4O_2024_05_13,
        StandardModel.GPT_4_TURBO_2024_04_09);
  }

  protected SarifToLLMForMultiOutcomeCodemod(
      final RuleSarif sarif,
      final OpenAIService openAI,
      final List<LLMRemediationOutcome> remediationOutcomes,
      final Model categorizationModel,
      final Model codeChangingModel) {
    super(sarif);
    this.openAI = Objects.requireNonNull(openAI);
    this.remediationOutcomes = Objects.requireNonNull(remediationOutcomes);
    if (remediationOutcomes.size() < 2) {
      throw new IllegalArgumentException("must have 2+ remediation outcome");
    }
    this.categorizationModel = Objects.requireNonNull(categorizationModel);
    this.codeChangingModel = Objects.requireNonNull(codeChangingModel);
  }

  @Override
  public CodemodFileScanningResult onFileFound(
      final CodemodInvocationContext context, final List<Result> results) {
    logger.info("processing: {}", context.path());

    List<CodemodChange> changes = new ArrayList<>();
    for (Result result : results) {
      Optional<CodemodChange> change = processResult(context, result);
      change.ifPresent(changes::add);
    }
    return CodemodFileScanningResult.withOnlyChanges(List.copyOf(changes));
  }

  private Optional<CodemodChange> processResult(
      final CodemodInvocationContext context, final Result result) {
    // short-circuit if the code is too long
    if (estimatedToExceedContextWindow(context)) {
      logger.debug("code too long: {}", context.path());
      return Optional.empty();
    }
    try {
      FileDescription file = FileDescription.from(context.path());

      final CategorizeResponse analysis = categorize(file, result);
      String outcomeKey = analysis.getOutcomeKey();
      logger.debug("outcomeKey: {}", outcomeKey);
      logger.debug("analysis: {}", analysis.getAnalysis());
      if (outcomeKey == null || outcomeKey.isBlank()) {
        logger.debug("unable to determine outcome");
        return Optional.empty();
      }
      Optional<LLMRemediationOutcome> outcome =
          remediationOutcomes.stream()
              .filter(oc -> oc.key().equals(analysis.outcomeKey))
              .findFirst();
      if (outcome.isEmpty()) {
        logger.debug("unable to find outcome for key: {}", analysis.outcomeKey);
        return Optional.empty();
      }
      LLMRemediationOutcome matchedOutcome = outcome.get();
      logger.debug("outcomeKey: {}", matchedOutcome.key());
      logger.debug("description: {}", matchedOutcome.description());
      if (!matchedOutcome.shouldApplyCodeChanges()) {
        logger.debug("Matched outcome suggests there should be no code changes");
        return Optional.empty();
      }

      CodeChangeResponse response = changeCode(file, result);
      logger.debug("outcome: {}", response.outcomeKey);
      logger.debug("analysis: {}", response.codeChange);

      // If our second look determined that there are no outcomes associated with code changes, we
      // should quit
      if (response.outcomeKey == null || outcomeKey.isEmpty()) {
        logger.debug("No outcomes detected");
        return Optional.empty();
      }

      List<String> codeChangingOutcomeKeys =
          remediationOutcomes.stream()
              .filter(LLMRemediationOutcome::shouldApplyCodeChanges)
              .map(LLMRemediationOutcome::key)
              .toList();

      boolean anyRequireCodeChanges = codeChangingOutcomeKeys.contains(response.outcomeKey);
      if (!anyRequireCodeChanges) {
        logger.debug("On second analysis, outcomes require no code changes");
        return Optional.empty();
      }

      String codeChange = response.codeChange;
      // If the LLM was unable to fix the threat, don't change the file.
      if (codeChange == null || codeChange.isEmpty()) {
        logger.info("unable to fix because diff not present: {}", context.path());
        return Optional.empty();
      }

      // Apply the fix.
      List<String> fixedLines = LLMDiffs.applyDiff(file.getLines(), codeChange);

      // Ensure the end result isn't wonky.
      Patch<String> patch = DiffUtils.diff(file.getLines(), fixedLines);
      if (patch.getDeltas().isEmpty()) {
        logger.error("empty patch: {}", patch);
        return Optional.empty();
      }

      try {
        // Replace the file with the fixed version.
        String fixedFile = String.join(file.getLineSeparator(), fixedLines);
        Files.writeString(context.path(), fixedFile, file.getCharset());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      // Report all the changes at their respective line number

      return Optional.of(createCodemodChange(result, response.line, response.fixDescription));
    } catch (IOException e) {
      logger.error("failed to process: {}", context.path(), e);
      throw new UncheckedIOException(e);
    } catch (Exception e) {
      logger.error("failed to process: {}", context.path(), e);
      throw e;
    }
  }

  /**
   * Estimates if the code is too long to analyze within the context windows of the given models.
   * This is only an estimate: the actual token count may be higher but won't be lower.
   *
   * @param context the current codemod invocation context
   * @return true when the prompts are estimated to exceed the context window for the models used in
   *     this codemod
   */
  private boolean estimatedToExceedContextWindow(final CodemodInvocationContext context) {
    // in both the threat analysis and code fix cases, the estimated user message size is dominated
    // by the code snippet, so use the code snippets as the floor
    final var estimatedUserMessage = new ChatRequestUserMessage(context.contents());
    for (final var model : List.of(categorizationModel, codeChangingModel)) {
      int tokenCount =
          model.tokens(
              List.of(
                  getSystemMessage().getContent(), estimatedUserMessage.getContent().toString()));
      // estimated token count doesn't include the function (~100 tokens) or the reply
      // (~200 tokens) so add those estimates before checking against window size
      tokenCount += 300;
      if (tokenCount > model.contextWindow()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Create a {@link CodemodChange} from the given code change data.
   *
   * @param line the line number of the change
   * @param fixDescription the description of the change
   */
  protected CodemodChange createCodemodChange(
      final Result result, final int line, final String fixDescription) {
    return CodemodChange.from(line, fixDescription);
  }

  /**
   * Instructs the LLM on how to assess the risk of the threat.
   *
   * @return The prompt.
   */
  protected abstract String getThreatPrompt();

  private CategorizeResponse categorize(final FileDescription file, final Result result)
      throws IOException {
    ChatRequestMessage systemMessage = getSystemMessage();
    ChatRequestMessage userMessage = getCategorizationUserMessage(file, result);
    return getCategorizationResponse(systemMessage, userMessage);
  }

  private CodeChangeResponse changeCode(final FileDescription file, final Result result)
      throws IOException {
    return getCodeChangeResponse(getSystemMessage(), getChangeCodeMessage(file, result));
  }

  private CategorizeResponse getCategorizationResponse(
      final ChatRequestMessage systemMessage, final ChatRequestMessage userMessage)
      throws IOException {
    return openAI.getResponseForPrompt(
        List.of(systemMessage, userMessage), categorizationModel, CategorizeResponse.class);
  }

  private CodeChangeResponse getCodeChangeResponse(
      final ChatRequestMessage systemMessage, final ChatRequestMessage userMessage)
      throws IOException {
    return openAI.getResponseForPrompt(
        List.of(systemMessage, userMessage), codeChangingModel, CodeChangeResponse.class);
  }

  private ChatRequestSystemMessage getSystemMessage() {
    return new ChatRequestSystemMessage(
        SYSTEM_MESSAGE_TEMPLATE.formatted(getThreatPrompt().strip()).strip());
  }

  /** Analyze a single SARIF result and get feedback. */
  private ChatRequestMessage getCategorizationUserMessage(
      final FileDescription file, final Result result) {
    Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
    int line = region.getStartLine();
    Integer column = region.getStartColumn();

    String outcomeDescriptions = formatOutcomeDescriptions(false);

    return new ChatRequestSystemMessage(
        CATEGORIZE_CODE_USER_MESSAGE_TEMPLATE
            .formatted(
                String.valueOf(line),
                column != null ? String.valueOf(column) : "(unknown)",
                outcomeDescriptions,
                file.getFileName(),
                file.formatLinesWithLineNumbers())
            .strip());
  }

  /**
   * Format the outcome descriptions for sending to the LLM. Should look something like this:
   *
   * <pre>
   * ===
   * Outcome: 'assignment_is_redundant':
   * Description: The variable is assigned and re-assigned to the same value. This is redundant and should be removed.
   * Code Changes Required: YES
   * Code Change Directions: Remove the initial assignment.
   * ===
   * Outcome: 'assignment_can_be_streamlined':
   * Description: The variable is created and then assigned in separate adjacent statements.
   * Code Changes Required: YES
   * Code Change Directions: Combine the two statements together.
   * ===
   * ...
   * </pre>
   */
  private String formatOutcomeDescriptions(boolean includeFixes) {
    String withFixTemplate =
        """
                    ============
                    Outcome: %s
                    Description: %s
                    Code Changes Required: YES
                    Code Change Directions For Outcome: %s
                    """;
    String withoutFixTemplate =
        """
                    ============
                    Outcome: %s
                    Description: %s
                    Code Changes Required: NO
                    """;

    Function<LLMRemediationOutcome, String> withFixProvider =
        (outcome) -> withFixTemplate.formatted(outcome.key(), outcome.description(), outcome.fix());
    Function<LLMRemediationOutcome, String> withoutFixProvider =
        (outcome) -> withoutFixTemplate.formatted(outcome.key(), outcome.description());
    return remediationOutcomes.stream()
            .map(oc -> includeFixes ? withFixProvider.apply(oc) : withoutFixProvider.apply(oc))
            .collect(Collectors.joining("\n"))
        + "\n============";
  }

  /**
   * Analyze a single SARIF result, and get the changed file back as well if it warrants change.
   *
   * @param file the file being analyzed
   * @param result the result to analyze
   * @return the message to send to the LLM
   */
  private ChatRequestMessage getChangeCodeMessage(final FileDescription file, final Result result) {

    Region region = result.getLocations().get(0).getPhysicalLocation().getRegion();
    String regionStr = "  Line " + region.getStartLine() + ", column " + region.getStartColumn();

    String outcomeDescriptions = formatOutcomeDescriptions(true);
    return new ChatRequestUserMessage(
        CHANGE_CODE_USER_MESSAGE_TEMPLATE
            .formatted(
                regionStr,
                outcomeDescriptions,
                file.getFileName(),
                file.formatLinesWithLineNumbers())
            .strip());
  }

  private static final String SYSTEM_MESSAGE_TEMPLATE =
      """
                  You are a security analyst bot. You are helping analyze code to assess its risk to a \
                  specific security threat. Your code change recommendations are safe and accurate.
                  %s
                  """;

  private static final String CATEGORIZE_CODE_USER_MESSAGE_TEMPLATE =
      """
                  Analyze ONLY line %s, column %s, and discern which "outcome" best describes the code. You should save your categorization analysis. You MUST ignore any other file contents, even if they look like they have issues.
                  Here are the possible outcomes:
                  %s

                  Return a JSON object as a response with the following keys in this order:
                    - analysis: A detailed analysis of how the analysis arrived at the outcome
                    - outcomeKey: The category of the analysis, or empty if the analysis could not be categorized
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

  private static final String CHANGE_CODE_USER_MESSAGE_TEMPLATE =
      """
                    The tool has cited the following location for you to analyze:
                    %s
                    Decide which "outcome" you want to place it in. Then, if that outcome requires code changes, make the changes as described in the Code Change Directions and save them. Here are the possible outcomes:
                    %s
                    Pick which outcome best describes the code. If you are making code changes, you MUST make the MINIMUM number of changes necessary to fix the issue.
                    - Each change MUST be syntactically correct.
                    - DO NOT change the file's formatting or comments.
                    - Create a diff patch for the changed file if and only if any of the outcomes require code changes.
                    - The patch must use the unified format with a header. Include the diff patch and a summary of the changes with your analysis.
                    If you the outcome says you should suppress a Semgrep finding in the code, insert a comment above it and put `// nosemgrep: <ruleid>`
                    Save your categorization and code change analysis when you're done.

                    Return a JSON object as a response with the following keys in this order:
                      - outcomeKey: The outcome key associated with this particular result location
                      - fixDescription: A short description of the code change. Required only if the file needs a change.
                      - codeChange: A diff patch in unified format. Required if any of the outcome keys indicate a change.
                      - line: The line in the file to which this analysis is related
                      - column: The column to which this analysis is related
                    --- %s
                    %s
                    """;

  static final class CodeChangeResponse {
    @JsonPropertyDescription(
        "The code change a diff patch in unified format. Required if any of the outcome keys indicate a change.")
    private String codeChange;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @JsonPropertyDescription("The line in the file to which this analysis is related")
    private int line;

    @JsonPropertyDescription("The column to which this analysis is related")
    private int column;

    @JsonPropertyDescription("The outcome key associated with this particular result location")
    private String outcomeKey;

    @JsonPropertyDescription(
        "A short description of the code change. Required only if the file needs a change.")
    private String fixDescription;

    public String getFixDescription() {
      return fixDescription;
    }

    public String getOutcomeKey() {
      return outcomeKey;
    }

    public int getLine() {
      return line;
    }

    public int getColumn() {
      return column;
    }

    public String getCodeChange() {
      return codeChange;
    }
  }
}
