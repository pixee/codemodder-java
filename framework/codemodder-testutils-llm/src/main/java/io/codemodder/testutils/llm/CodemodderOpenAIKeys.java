package io.codemodder.testutils.llm;

/**
 * A utility class to check that the {@code CODEMODDER_OPENAI_API_KEY} environment variable is set
 * for JUnit tests.
 */
@SuppressWarnings("unused")
public final class CodemodderOpenAIKeys {

  /**
   * Return true if and only if we have a non-empty {@code CODEMODDER_OPENAI_API_KEY} environment
   * variable.
   */
  public static boolean isAvailable() {
    String key = System.getenv("CODEMODDER_OPENAI_API_KEY");
    //return key != null && !key.isBlank();
    if(key == null || key.isBlank()){
      // Log a warning or throw an exception
      System.out.println("WARNING: CODEMODDER_OPENAI_API_KEY is missing or empty.")
        // or throw new IllegalStateException("CODEMODDER_OPENAI_API_KEY is missing or empty."
        return false;
  }
    return true;
  }
}
