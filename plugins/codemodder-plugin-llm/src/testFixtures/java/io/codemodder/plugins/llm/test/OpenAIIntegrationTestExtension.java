package io.codemodder.plugins.llm.test;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Extension for writing OpenAI integration tests. */
public final class OpenAIIntegrationTestExtension implements ExecutionCondition {
  /**
   * The OpenAI integration tests only run when the test suite has been configured with an OpenAI
   * API key. This is a convenience to developers who may want to avoid running these relatively
   * slow and expensive tests. However, we always want to run these tests in CI; therefore, this
   * check includes backstop logic that ensures we never accidentally skip these tests in CI.
   *
   * <p>This extension reads the OpenAI key from the standard environment variable {@code
   * CODEMODDER_OPENAI_API_KEY} vs the recommended method of reading configuration from the JUnit
   * context. This is necessary, because the code under test reads the OpenAI key from an
   * environment variable directly vs requiring the key to be injected, and this makes test
   * configuration less flexible than it could be. Improving this is out-of-scope for the initial
   * development of this extension.
   *
   * @return a result indicating whether the test should be enabled or disabled
   */
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(final ExtensionContext ignored) {
    String key = System.getenv("CODEMODDER_OPENAI_API_KEY");
    final var available = key != null && !key.isBlank();
    if (System.getenv("CI") != null && !available) {
      throw new IllegalStateException(
          "The CODEMODDER_OPENAI_API_KEY environment variable must be set in the CI/CD environment.");
    }
    return available
        ? ConditionEvaluationResult.enabled(
            "Using OpenAI API key from env var CODEMODDER_OPENAI_API_KEY")
        : ConditionEvaluationResult.disabled(
            "OpenAI API key not found in env var CODEMODDER_OPENAI_API_KEY");
  }
}
