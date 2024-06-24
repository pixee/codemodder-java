package io.codemodder.codemods;

import io.codemodder.plugins.llm.test.LLMVerifyingCodemodTestMixin;
import io.codemodder.plugins.llm.test.OpenAIIntegrationTest;
import io.codemodder.testutils.Metadata;

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
