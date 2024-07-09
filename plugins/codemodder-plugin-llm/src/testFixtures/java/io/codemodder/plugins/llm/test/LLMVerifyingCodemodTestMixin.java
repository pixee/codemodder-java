package io.codemodder.plugins.llm.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import io.codemodder.EncodingDetector;
import io.codemodder.plugins.llm.Model;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.StandardModel;
import io.codemodder.testutils.CodemodTestMixin;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** A mixin for codemod tests that use the LLM framework to change the code. */
public interface LLMVerifyingCodemodTestMixin extends CodemodTestMixin {

  /**
   * If a codemod's changes don't <em>exactly</em> match the {@code .java.after} file, the LLM will
   * use these requirements to assess whether the changes are similar enough to pass the test.
   */
  String getRequirementsPrompt();

  /**
   * @return GPT model to use for the test harness to verify the codemod's changes
   */
  default Model model() {
    return StandardModel.GPT_4O;
  }

  @Override
  default void verifyTransformedCode(final Path before, final Path expected, final Path after)
      throws IOException {
    String expectedCode = Files.readString(expected, getCharset(expected));
    String transformedJavaCode = Files.readString(after, getCharset(after));

    // Verify the changes.
    if (!expectedCode.equals(transformedJavaCode)) {
      // If the changes aren't identical, ask the LLM if they're close enough.
      Assessment assessment = assessChanges(before, after, expected);
      assertThat(assessment.getAnalysis(), assessment.getResult(), is(AssessmentResult.PASS));

      // If the LLM thinks they're close enough, print out the analysis for troubleshooting.
      System.out.println(assessment.getAnalysis());
    }
  }

  private Assessment assessChanges(
      final Path before, final Path actualAfter, final Path expectedAfter) throws IOException {
    OpenAIService openAI = OpenAIService.fromOpenAI(System.getenv("CODEMODDER_OPENAI_API_KEY"));
    return openAI.getResponseForPrompt(
        List.of(
            new ChatRequestSystemMessage(
                SYSTEM_MESSAGE_TEMPLATE
                    .formatted(
                        getRequirementsPrompt().strip(),
                        getUnifiedDiff(before, expectedAfter).strip())
                    .strip()),
            new ChatRequestUserMessage(
                USER_MESSAGE_TEMPLATE
                    .formatted(getUnifiedDiff(before, actualAfter).strip())
                    .strip())),
        model().id(),
        Assessment.class);
  }

  private String getUnifiedDiff(final Path original, final Path revised) throws IOException {
    return String.join(
            "\n",
            UnifiedDiffUtils.generateUnifiedDiff(
                original.getFileName().toString(),
                original.getFileName().toString(),
                readAllLines(original),
                diff(original, revised),
                5))
        + "\n";
  }

  private Patch<String> diff(final Path original, final Path revised) throws IOException {
    return DiffUtils.diff(readAllLines(original), readAllLines(revised));
  }

  private List<String> readAllLines(final Path path) throws IOException {
    return List.of(Files.readString(path, getCharset(path)).split("\\R", -1));
  }

  private Charset getCharset(final Path path) throws IOException {
    return Charset.forName(EncodingDetector.create().detect(path).orElse("UTF-8"));
  }

  String SYSTEM_MESSAGE_TEMPLATE =
      """
            You are a software engineer bot. You are helping assess a Java coding assignment given to an \
            interviewee.

            The interviewee was given code and asked to modify it to meet these requirements:
            %s

            A PASS example:
            ```diff
            %s
            ```

            You will be given the interviewee's solution in unified diff format. Analyze the changes \
            line-by-line, compare them to the PASS example, and assess whether they PASS or FAIL the \
            assignment. If the changes have any syntax errors or are made in a block of code that does \
            not meet the requirements, they automatically FAIL.

            Return a JSON object with the following fields in order:
            - analysis: A detailed analysis of how the candidate's solution was assessed.
            - result: The result of the assessment, either PASS or FAIL.
            """;

  String USER_MESSAGE_TEMPLATE =
      """
            ```diff
            %s
            ```
            """;

  enum AssessmentResult {
    PASS,
    FAIL;
  }

  final class Assessment {
    @JsonPropertyDescription("A detailed analysis of how the candidate's solution was assessed.")
    @JsonProperty(required = true)
    private String analysis;

    @JsonPropertyDescription("The result of the assessment, either PASS or FAIL.")
    @JsonProperty(required = true)
    private AssessmentResult result;

    public String getAnalysis() {
      return analysis;
    }

    public AssessmentResult getResult() {
      return result;
    }
  }
}
