package io.codemodder.testutils.llm;

/**
 * A utility class to check that the {@code CODEMODDER_OPENAI_API_KEY} environment variable is set
 * for JUnit tests.
 */
@SuppressWarnings("unused")
public final class CodemodderOpenAIKeys {

  private CodemodderOpenAIKeys() {}

  /**
   * Return true if and only if we have a non-empty {@code CODEMODDER_OPENAI_API_KEY} environment
   * variable.
   */
  public static boolean isAvailable() {
    String key = System.getenv("CODEMODDER_OPENAI_API_KEY");
    return key != null && !key.isBlank();
  }
}
