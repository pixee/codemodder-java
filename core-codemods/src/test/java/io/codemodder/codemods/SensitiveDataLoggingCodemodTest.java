package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;

@Metadata(
    codemodType = SensitiveDataLoggingCodemod.class,
    testResourceDir = "sensitive-data-logging",
    dependencies = {})
final class SensitiveDataLoggingCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Remove ALL lines that log sensitive data at INFO or higher severities.
        """;
  }
}
