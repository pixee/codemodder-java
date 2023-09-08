package io.codemodder.plugins.llm;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

/** A set of utilities around LLM tokens. */
public final class Tokens {

  private Tokens() {}

  /**
   * Estimates the number of tokens the messages will consume when passed to the {@code
   * gpt-3.5-turbo-0613} or {@code gpt-4-0613} models.
   *
   * <p>This does not yet support estimating the number of tokens the functions will consume, since
   * the <a
   * href="https://community.openai.com/t/how-to-calculate-the-tokens-when-using-function-call/266573/1">unofficial
   * solutions</a> are brittle.
   *
   * <p>We should be able to replace this with {@code TikTokensUtil.tokens} when the <a
   * href="https://github.com/TheoKanning/openai-java/pull/311">feature</a> is released.
   *
   * @param messages The messages.
   * @return The number of tokens.
   * @see <a
   *     href="https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb">How
   *     to count tokens with tiktoken</a>
   */
  public static int countTokens(final List<ChatMessage> messages) {
    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    int count = 0;
    for (ChatMessage message : messages) {
      count += 3; // Both gpt-3.5-turbo-0613 and gpt-4-0613 consume 3 tokens per message.
      count += encoding.countTokens(message.getContent());
      count += encoding.countTokens(message.getRole());
    }
    count += 3; // Every reply is primed with <|start|>assistant<|message|>.

    return count;
  }
}
