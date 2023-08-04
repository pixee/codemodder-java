package io.codemodder.codemods;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.javaparser.JavaParser;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest.ChatCompletionRequestFunctionCall;
import com.theokanning.openai.completion.chat.ChatFunction;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.FunctionExecutor;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodExecutor;
import io.codemodder.CodemodIdPair;
import io.codemodder.CodemodLoader;
import io.codemodder.EncodingDetector;
import io.codemodder.IncludesExcludes;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.testutils.Metadata;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.api.io.TempDir;

/**
 * An {@code LLMAssistedCodemodTest} tests each file with extension {@code .java.before} in the
 * {@link Metadata#testResourceDir()} test resource directory (including subdirectories) by
 * executing the {@link Metadata#codemodType()} LLM-assisted codemod and verifying the changes.
 *
 * <p>If a file being tested is not paired with a {@code .java.after} file, the test will verify
 * that the LLM-assisted codemod does <em>not</em> change the file.
 *
 * <p>If the {@code CODEMODDER_OPENAI_API_KEY} environment variable is not set, the tests will be
 * disabled.
 */
public abstract class LLMAssistedCodemodTest {

  @TempDir Path tempDir;

  /**
   * If a codemod's changes don't <em>exactly</em> match the {@code .java.after} file, the LLM will
   * use these requirements to assess whether the changes are similar enough to pass the test.
   */
  protected abstract String getRequirementsPrompt();

  @TestFactory
  @EnabledIfEnvironmentVariable(named = "CODEMODDER_OPENAI_API_KEY", matches = ".+")
  Stream<DynamicTest> generateTestCases() throws IOException {
    Metadata metadata = getClass().getAnnotation(Metadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException("Test class must be annotated with @Metadata");
    }

    // Test all files with the `.java.before` extension in `testResourceDir`.
    Stream<Path> inputStream =
        Files.walk(Path.of("src/test/resources/", metadata.testResourceDir()))
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java.before"));

    Function<Path, String> displayNameGenerator = Path::toString;

    ThrowingConsumer<Path> testExecutor = path -> verifyCodemod(metadata.codemodType(), path);

    return DynamicTest.stream(inputStream, displayNameGenerator, testExecutor);
  }

  private void verifyCodemod(final Class<? extends CodeChanger> codemodType, final Path testFile)
      throws IOException {
    System.out.println("testing: " + testFile);

    // Copy the test file to the temp directory (we need to specify `REPLACE_EXISTING` since
    // dynamic tests share a `tempDir`).
    Path tempTestFile = tempDir.resolve("Test.java");
    Files.copy(testFile, tempTestFile, REPLACE_EXISTING);

    // Load the codemod.
    CodemodLoader loader = new CodemodLoader(List.of(codemodType), tempDir);
    CodemodIdPair codemod = loader.getCodemods().get(0);

    // Execute the codemod on the test file.
    CodeTFResult result = executeCodemod(codemod, tempTestFile);

    // Verify that the test file was processed successfully.
    assertThat(result.getFailedFiles(), is(empty()));

    Path expectedFile =
        testFile.resolveSibling(testFile.getFileName().toString().replace(".before", ".after"));

    // If there is no `.after` file, verify that no changes were made.
    if (Files.notExists(expectedFile)) {
      assertThat(result.getChangeset(), is(empty()));
      assertThat(diff(testFile, tempTestFile).getDeltas(), is(empty()));
      return;
    }

    // Verify that we have a report.
    assertThat(result.getSummary(), is(not(blankOrNullString())));
    assertThat(result.getDescription(), is(not(blankOrNullString())));
    assertThat(result.getReferences(), is(not(empty())));

    // Verify that we have a changeset for the file.
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size(), is(1));
    assertThat(changeset.get(0).getChanges(), is(not(empty())));

    // Verify that each change has a line number and description.
    for (CodeTFChange change : changeset.get(0).getChanges()) {
      assertThat(change.getLineNumber(), is(greaterThan(0)));
      assertThat(change.getDescription(), is(not(blankOrNullString())));
    }

    // Verify the changes.
    if (!Files.readString(tempTestFile, getCharset(tempTestFile))
        .equals(Files.readString(expectedFile, getCharset(expectedFile)))) {
      // If the changes aren't identical, ask the LLM if they're close enough.
      Assessment assessment = assessChanges(testFile, tempTestFile, expectedFile);
      assertThat(assessment.getAnalysis(), assessment.getResult(), is(AssessmentResult.PASS));

      // If the LLM thinks they're close enough, print out the analysis for troubleshooting.
      System.out.println(assessment.getAnalysis());
    }

    // Re-execute the codemod on the test file and verify that it wasn't changed again.
    CodeTFResult result2 = executeCodemod(codemod, tempTestFile);
    assertThat(result2.getFailedFiles(), is(empty()));
    assertThat(result2.getChangeset(), is(empty()));
  }

  private CodeTFResult executeCodemod(CodemodIdPair codemod, Path file) {
    CodemodExecutor executor =
        CodemodExecutor.from(
            tempDir,
            IncludesExcludes.any(),
            codemod,
            List.of(),
            List.of(),
            CachingJavaParser.from(new JavaParser()),
            EncodingDetector.create());
    return executor.execute(List.of(file));
  }

  private Patch<String> diff(final Path original, final Path revised) throws IOException {
    return DiffUtils.diff(readAllLines(original), readAllLines(revised));
  }

  private String getUnifiedDiff(final Path original, final Path revised) throws IOException {
    return String.join(
            "\n",
            UnifiedDiffUtils.generateUnifiedDiff(
                original.getFileName().toString(),
                original.getFileName().toString(),
                readAllLines(original),
                diff(original, revised),
                3))
        + "\n";
  }

  private List<String> readAllLines(final Path path) throws IOException {
    return List.of(Files.readString(path, getCharset(path)).split("\\R", -1));
  }

  private Charset getCharset(final Path path) throws IOException {
    return Charset.forName(EncodingDetector.create().detect(path).orElse("UTF-8"));
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
                                getRequirementsPrompt(), getUnifiedDiff(before, expectedAfter))
                            .strip()),
                    new ChatMessage(
                        ChatMessageRole.USER.value(),
                        USER_MESSAGE_TEMPLATE
                            .formatted(getUnifiedDiff(before, actualAfter))
                            .strip())))
            .functions(functionExecutor.getFunctions())
            .functionCall(ChatCompletionRequestFunctionCall.of(function.getName()))
            .temperature(0D)
            .build();

    OpenAIService openAI = new OpenAIService(System.getenv("CODEMODDER_OPENAI_API_KEY"));
    ChatMessage response = openAI.createChatCompletion(request).getChoices().get(0).getMessage();
    return functionExecutor.execute(response.getFunctionCall());
  }

  private static final String SYSTEM_MESSAGE_TEMPLATE =
      """
      You are a software engineer bot. You are helping assess a Java coding assignment given to an \
      interviewee.

      The interviewee was given code and asked to modify it to meet these requirements:
      %s

      An example of a PASS:
      ```diff
      %s
      ```

      You will be given the interviewee's solution in unified diff format. Analyze the changes \
      line-by-line, compare them to the example, and assess whether they PASS or FAIL the \
      assignment. If the changes have any syntax errors, they automatically FAIL.
      """;

  private static final String USER_MESSAGE_TEMPLATE =
      """
      ```diff
      %s
      ```
      """;
}

enum AssessmentResult {
  PASS,
  FAIL;
}

class Assessment {
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
