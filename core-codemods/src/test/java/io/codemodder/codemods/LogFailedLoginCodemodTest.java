package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;

@Metadata(
    codemodType = LogFailedLoginCodemod.class,
    testResourceDir = "log-failed-login",
    dependencies = {})
public final class LogFailedLoginCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Log a message when a login attempt fails.
        - Use the same log message style as the rest of the code.
        """;
  }
}
