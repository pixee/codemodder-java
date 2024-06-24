package io.codemodder.plugins.llm;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.util.List;

/**
 * Internal model for a GPT language model. Helps to colocate model-specific logic e.g. token
 * counting.
 */
public interface Model {

  /**
   * @return well-known model ID e.g. gpt-3.5-turbo-0125
   */
  String id();

  /**
   * @return maximum size of the context window supported by this model
   */
  int contextWindow();

  /**
   * Estimates the number of tokens the messages will consume when passed to this model. The
   * estimate can vary based on the model.
   *
   * @param messages the list of messages for which to estimate token usage
   * @return estimated tokens that would be consumed by the model
   */
  int tokens(List<ChatMessage> messages);
}
