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
   *
   * @throws IllegalStateException when running in a CI/CD environment and the key is not available.
   *     This is backstop to ensure that all expected tests run in CI/CD.
   */
  public static boolean isAvailable() {
    String key = System.getenv("CODEMODDER_OPENAI_API_KEY");
    final var available = key != null && !key.isBlank();
    if (System.getenv("CI") != null && !available) {
      throw new IllegalStateException(
          "The CODEMODDER_OPENAI_API_KEY environment variable must be set in the CI/CD environment.");
    }
    return available;
  }
}
