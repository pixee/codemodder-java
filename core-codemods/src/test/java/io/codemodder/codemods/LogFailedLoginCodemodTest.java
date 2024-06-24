package io.codemodder.codemods;

import io.codemodder.plugins.llm.test.LLMVerifyingCodemodTestMixin;
import io.codemodder.plugins.llm.test.OpenAIIntegrationTest;
import io.codemodder.testutils.Metadata;

/**
 * Tests for the {@link LogFailedLoginCodemod}.
 *
 * <p>Test cases that should not have code changes:
 *
 * <dl>
 *   <dt>safe/AuthProvider.java.before
 *   <dd>Describes a type that performs authentication, but no authentication implemented here.
 *   <dt>safe/JaaSAuthenticationBroker.java.before
 *   <dd>Throws exceptions that indicate failed login attempts.
 *   <dt>safe/LoginServlet.java.before
 *   <dd>logs authentication failures at the WARN level.
 *   <dt>safe/Main.java.before
 *   <dd>Logs a message when authentication fails.
 *   <dt>safe/MainPrint.before
 *   <dd>prints to the console when a login attempt fails.
 *   <dt>safe/Queue.java.before
 *   <dd>is too large to be analyzed.
 * </dl>
 *
 * Test cases that should have code changes:
 *
 * <dl>
 *   <dt>unsafe/LoginServlet.java.before
 *   <dd>lacks a log statement before returning unauthorized response
 *   <dt>unsafe/LoginValidate.java.before
 *   <dd>lacks a print statement before redirecting to error page.
 *   <dt>unsafe/MainFame.java.before
 *   <dd>lacks a log statement before showing the dialog.
 *   <dt>unsafe/SaltedHashLoginModule
 *   <dd>lacks a log statement before returning the authenticated decision. That is the correct
 *       place to log, because it has the username in scope.
 */
@Metadata(
    codemodType = LogFailedLoginCodemod.class,
    testResourceDir = "log-failed-login",
    dependencies = {})
@OpenAIIntegrationTest
public final class LogFailedLoginCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Log a message when a login attempt fails.
        - Use the same log message style as the rest of the code.
        """;
  }
}
