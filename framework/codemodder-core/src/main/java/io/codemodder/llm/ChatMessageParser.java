package io.codemodder.llm;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

/**
 * This type is responsible for loading "training" data in our ChatTXT format. This is a simple
 * format that contains the same data as the OpenAI ChatML format, but is easier to work with. As
 * opposed to ChatML, writing multiline content like code is easy in ChatTXT.
 *
 * <p>The ChatTXT format requires a <a
 * href="https://help.openai.com/en/articles/7042661-chatgpt-api-transition-guide">role</a> (one of
 * "system", "assistant" and "role") and some content as shown below:
 *
 * <pre>
 * ---- role: system ----
 *
 * You are a helpful assistant that help people change their code for the better...
 *
 * ---- end ----
 * </pre>
 *
 * <p>You can use this to load some system instructions, and pre-program some fine-tuning examples
 * between the user and assistant. This is critical because gpt-3.5-turbo, as of the time of
 * writing, doesn't have the ability to do any fine-tuning.
 *
 * <p>Note that you can have many messages loaded from a single file, each with it's own 'role' and
 * 'end' boundary markers.
 */
public interface ChatMessageParser {

  /**
   * Parse a list of chat messages from a ChatTxt string.
   *
   * @param chatTxt the chatTxt to parse
   * @return the list of chat messages
   */
  List<ChatMessage> fromText(String chatTxt) throws IOException;

  /**
   * Parse a list of chat messages from a classpath resource, calling {@link
   * ChatMessageParser#fromText(String)} on its contents.
   */
  default List<ChatMessage> fromClasspathResource(final String resourcePath) throws IOException {
    InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
    String chatTxt =
        IOUtils.toString(Objects.requireNonNull(resourceAsStream), StandardCharsets.UTF_8);
    return fromText(chatTxt);
  }

  static ChatMessageParser createDefault() {
    return chatTxt -> {
      List<ChatMessage> messages = new ArrayList<>();
      Pattern p = Pattern.compile("---- role: ([a-z]+) ----\n(.*?)\n---- end ----", Pattern.DOTALL);
      Matcher matcher = p.matcher(chatTxt);
      while (matcher.find()) {
        String role = matcher.group(1);
        String text = matcher.group(2);
        ChatMessage message = new ChatMessage(role, text);
        messages.add(message);
      }
      return Collections.unmodifiableList(messages);
    };
  }
}
