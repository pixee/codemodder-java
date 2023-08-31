package io.codemodder.testutils.llm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.EncodingDetector;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.testutils.CodemodTestMixin;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** A mixin for codemod tests that use the LLM framework to change the code. */
@EnabledIfEnvironmentVariable(named = "CODEMODDER_OPENAI_API_KEY", matches = ".+")
public interface LLMVerifyingCodemodTestMixin extends CodemodTestMixin {

  /**
   * If a codemod's changes don't <em>exactly</em> match the {@code .java.after} file, the LLM will
   * use these requirements to assess whether the changes are similar enough to pass the test.
   */
  String getRequirementsPrompt();

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

  @Override
  default void verifyRetransformedCode(
      final Path before, final Path expected, final Path retransformedAfter) throws IOException {
    verifyTransformedCode(before, expected, retransformedAfter);
  }

  private Assessment assessChanges(
      final Path before, final Path actualAfter, final Path expectedAfter) throws IOException {
    // Create a function to get the LLM to return a structured response.
    ChatFunction function =
        ChatFunction.builder()
            .name("save_assessment")
            .description("Saves an assessment.")
            .executor(Assessment.class, c -> c) // Return the instance when executed.
            .build();

    FunctionExecutor functionExecutor = new FunctionExecutor(Collections.singletonList(function));

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .model("gpt-3.5-turbo-0613")
            .messages(
                List.of(
                    new ChatMessage(
                        ChatMessageRole.SYSTEM.value(),
                        SYSTEM_MESSAGE_TEMPLATE
                            .formatted(
                                getRequirementsPrompt().strip(),
                                getUnifiedDiff(before, expectedAfter).strip())
                            .strip()),
                    new ChatMessage(
                        ChatMessageRole.USER.value(),
                        USER_MESSAGE_TEMPLATE
                            .formatted(getUnifiedDiff(before, actualAfter).strip())
                            .strip())))
            .functions(functionExecutor.getFunctions())
            .functionCall(
                ChatCompletionRequest.ChatCompletionRequestFunctionCall.of(function.getName()))
            .temperature(0D)
            .build();

    OpenAIService openAI = new OpenAIService(System.getenv("CODEMODDER_OPENAI_API_KEY"));
    ChatMessage response = openAI.createChatCompletion(request).getChoices().get(0).getMessage();
    return functionExecutor.execute(response.getFunctionCall());
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
