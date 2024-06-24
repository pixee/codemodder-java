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
   * Estimates the number of tokens the messages will consume.
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
   * @param tokensPerMessage The number of tokens consumed per message by the given model.
   * @param encodingType The encoding type used by the model.
   * @return The number of tokens.
   * @see <a
   *     href="https://github.com/openai/openai-cookbook/blob/main/examples/How_to_count_tokens_with_tiktoken.ipynb">How
   *     to count tokens with tiktoken</a>
   */
  public static int countTokens(
      final List<ChatMessage> messages,
      final int tokensPerMessage,
      final EncodingType encodingType) {
    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    Encoding encoding = registry.getEncoding(encodingType);

    int count = 0;
    for (ChatMessage message : messages) {
      count += tokensPerMessage;
      count += encoding.countTokens(message.getContent());
      count += encoding.countTokens(message.getRole());
    }
    count += tokensPerMessage; // Every reply is primed with <|start|>assistant<|message|>.

    return count;
  }
}
