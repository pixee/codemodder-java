package io.codemodder.plugins.llm;

import com.knuddels.jtokkit.api.EncodingType;
import java.util.List;

/** Well-known GPT models used in Codemod development. */
public enum StandardModel implements Model {
  GPT_3_5_TURBO_0125("gpt-3.5-turbo-0125", 16_385) {
    @Override
    public int tokens(final List<String> messages) {
      return Tokens.countTokens(messages, 3, EncodingType.CL100K_BASE);
    }
  },
  GPT_4_0613("gpt-4-0613", 8_192) {
    @Override
    public int tokens(final List<String> messages) {
      return Tokens.countTokens(messages, 3, EncodingType.CL100K_BASE);
    }
  },
  GPT_4_TURBO_2024_04_09("gpt-4-turbo-2024-04-09", 128_000) {
    @Override
    public int tokens(final List<String> messages) {
      return Tokens.countTokens(messages, 3, EncodingType.CL100K_BASE);
    }
  },
  GPT_4O_2024_05_13("gpt-4o-2024-05-13", 128_000) {
    /**
     * This is wrong - we copy / pasted from GPT 3.5 while we await GPT-4o token counting support <a
     * href="https://github.com/knuddelsgmbh/jtokkit/issues/96">from upstream utility</a>.
     */
    @Override
    public int tokens(final List<String> messages) {
      return Tokens.countTokens(messages, 3, EncodingType.CL100K_BASE);
    }
  };

  private final String id;
  private final int contextWindow;

  StandardModel(final String id, final int contextWindow) {
    this.id = id;
    this.contextWindow = contextWindow;
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public int contextWindow() {
    return contextWindow;
  }
}
