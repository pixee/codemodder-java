package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMAssistedCodemodTest;

@Metadata(
    codemodType = SensitiveDataLoggingCodemod.class,
    testResourceDir = "sensitive-data-logging",
    dependencies = {})
final class SensitiveDataLoggingCodemodTest extends LLMAssistedCodemodTest {
  @Override
  protected String getRequirementsPrompt() {
    return """
        - Remove ALL lines that log sensitive data at INFO or higher severities.
        """;
  }
}
