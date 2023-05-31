package io.codemodder.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RawFileChanger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.jetbrains.annotations.ApiStatus;

/**
 * A ready-to-extend type for writing an OpenAI-based codemod based on gpt-3.5-turbo. The type
 * provides an opinionated framework that boxes the LLM interaction and prompting into a formula
 * that has appeared to produce predictable and safe changes.
 *
 * <p>Here's the steps to writing an LLM-based codemod with this type:
 *
 * <ol>
 *   <li>Decide what role you want the LLM to play: finding stuff, changing stuff, or both.
 *   <li>Use typical programming patterns as best you can to limit the "lines of interest" (ideally
 *       to zero in most file analyses)
 *   <li>Model the changes you'd want to see in a fine-tuning ChatTXT file
 *   <li>Provide an API key for execution in your environment variable
 * </ol>
 */
@ApiStatus.Experimental
public abstract class OpenAIGPT35TurboCodeChanger extends RawFileChanger {

  private final ObjectMapper mapper;
  private final Provider<OpenAiService> openAIServiceProvider;
  protected final ChatMessageParser chatMessageParser;
  private final List<ChatMessage> training;

  public OpenAIGPT35TurboCodeChanger() {
    this(
        () -> {
          String apiKey = System.getenv("CODEMODDER_OPENAI_API_KEY");
          if (apiKey == null) {
            throw new IllegalArgumentException(
                "CODEMODDER_OPENAI_API_KEY environment variable must be set");
          }
          return new OpenAiService(apiKey, Duration.ofSeconds(60));
        });
  }

  public OpenAIGPT35TurboCodeChanger(final Provider<OpenAiService> apiKeyProvider) {
    this.openAIServiceProvider = Objects.requireNonNull(apiKeyProvider);
    this.chatMessageParser = ChatMessageParser.createDefault();
    this.mapper = new ObjectMapper();

    String resourcePath =
        "/"
            + OpenAIGPT35TurboCodeChanger.class.getPackageName().replace('.', '/')
            + "/global_prompt.md";
    InputStream trainingPrompt =
        Objects.requireNonNull(getClass().getResourceAsStream(resourcePath));

    try {
      String globalPromptText = new String(trainingPrompt.readAllBytes());
      this.training = chatMessageParser.fromText(globalPromptText);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Before the user prompt in {@link OpenAIGPT35TurboCodeChanger#getUserPrompt()} is sent, you have
   * to send additional fine-tuning messages by overriding this method to provide more messages.
   *
   * <p>The messages you send should show the interaction between the user and the assistant where
   * the assistant is returning messages in the form of JSON that match your expectations. Remember
   * that for gpt-3.5-turbo, you must keep the entire exchange beneath 4k tokens (at the time of
   * writing.)
   *
   * <p>You can use a classpath resource containing ChatTXT and {@link ChatMessageParser} to make
   * this easy and portable.
   *
   * <p>Here's an example of a ChatTXT fine-tuning message that shows the user telling the assistant
   * to change all methods named {@code A()} to {@code B()}.
   *
   * <pre>{@code
   * ---- role: user ----
   * How about this code? Only consider lines 3, 5 and 6. Ignore anything not on those lines.
   *
   * ```
   * 1: package com.acme;
   * 2: class AcmeType {
   * 3:   void A() { }
   * 4:   void A(String s) { }
   * 5:   void A(InputStream is) { }
   * 6:   void B(File f) { }
   * 7: }
   * ```
   * ---- end ----
   * ---- role: assistant ----
   *
   * {
   *     "requiresChange": "true",
   *     "analyses": [
   *          {
   *           "line": "3",
   *           "analysis" : "The method name matched, so a transform was performed",
   *           "fixed" : "true"
   *          },
   *          {
   *           "line": "5",
   *           "analysis" : "The method name matched, so a transform was performed",
   *           "fixed" : "true"
   *          },
   *          {
   *           "line": "6",
   *           "analysis" : "The method name did not match, so a transform was skipped",
   *           "fixed" : "false"
   *          }
   *     ]
   * }
   *
   * ---- end ----
   * }</pre>
   *
   * <p>The more intricate the transform, the more likely you are to need to send multiple
   * fine-tuning messages. You can probably get away fitting ~10 training fine-tuning messages while
   * still reserving room for the user prompt.
   *
   * @see LLMCodeFixResponse
   * @see LLMLineAnalysis
   * @see ChatMessageParser#fromText(String)
   */
  protected List<ChatMessage> getFineTuning() throws IOException {
    String resourcePath = "/" + getClass().getName().replace('.', '/') + "/prompt.md";
    return this.chatMessageParser.fromClasspathResource(resourcePath);
  }

  @Override
  public List<CodemodChange> visitFile(final CodemodInvocationContext context) throws IOException {
    Set<Integer> linesOfInterest = findLinesOfInterest(context);
    if (linesOfInterest.isEmpty()) {
      return List.of();
    }

    String code = Files.readString(context.path());
    OpenAiService service = openAIServiceProvider.get();
    String lines = linesOfInterest.stream().map(String::valueOf).collect(Collectors.joining(","));
    String codemodSpecificPrompt = getUserPrompt();
    String prompt =
        String.format(
            "%s. Please only consider lines %s. Ignore anything not on those lines. Unless I previously said otherwise, please don't insert any comments into the new code. \n\n```\n%s\n```",
            codemodSpecificPrompt, lines, code);
    ChatMessage ask = new ChatMessage("user", prompt);
    List<ChatMessage> allMessages = new ArrayList<>(training);
    allMessages.addAll(getFineTuning());
    allMessages.add(ask);

    ChatCompletionRequest chat =
        ChatCompletionRequest.builder()
            .temperature(0D)
            .model("gpt-3.5-turbo")
            .messages(allMessages)
            .build();
    ChatCompletionResult chatCompletion = service.createChatCompletion(chat);
    List<ChatCompletionChoice> choices = chatCompletion.getChoices();
    ChatCompletionChoice chatCompletionChoice = choices.get(0);
    String responseText = chatCompletionChoice.getMessage().getContent();
    LLMCodeFixResponse codeFixResponse = mapper.readValue(responseText, LLMCodeFixResponse.class);

    if (!codeFixResponse.isChangeRequired()) {
      return List.of();
    }

    String fix = codeFixResponse.getFix();
    // if the last line should be a newline, and it's missing, re-add it
    if (code.endsWith("\n") && !fix.endsWith("\n")) {
      fix += "\n";
    }

    Files.writeString(context.path(), fix);

    return codeFixResponse.getAnalyses().stream()
        .filter(LLMLineAnalysis::isFixed)
        .map(LLMLineAnalysis::getLine)
        .map(CodemodChange::from)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Give the individual prompt to use to explain the analysis we want it ot perform, and if change
   * we want to make. You only need discuss the search and transform you are interested in making.
   * The type and fine-tuning will establish the format of the back-and-forth. Some examples of what
   * user prompts may be:
   * <li>
   */
  protected abstract String getUserPrompt();

  /**
   * Find the lines that require more inspection and/or transformation by the LLM for this codemod.
   * The better this code is at "pruning the tree" of possible suspects for inspection and/or
   * transformation, the better the overall performance of the codemod will be. The calls to LLMs
   * are extremely slow (3-60 seconds) in comparison to the local CPU cycles spent here using regex
   * or AST parsing to find the lines of interest.
   *
   * <p>Consider if we wanted a codemod that replaces all uses of API A with API B. Because the
   * change is complicated and requires multiple statements to be inserted and removed, we want an
   * LLM to do it. This change will be in very few files in a codebase, so we don't want to send
   * every file to the LLM and ask, "if API A is here, change it to API B". We instead implement
   * this method to find all the places in a file where API A is used, and tell the LLM to only
   * focus on those lines. If no lines are found, and this method returns an empty {@link Set}, then
   * there will be no interaction with the LLM and performance will be much more acceptable.
   *
   * <p>There is another purpose of returning an accurate set of lines here -- these lines will be
   * used to generate the change report.
   */
  protected abstract Set<Integer> findLinesOfInterest(CodemodInvocationContext context)
      throws IOException;
}
