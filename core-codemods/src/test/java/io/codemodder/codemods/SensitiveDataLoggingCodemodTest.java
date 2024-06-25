package io.codemodder.codemods;

import io.codemodder.plugins.llm.test.LLMVerifyingCodemodTestMixin;
import io.codemodder.plugins.llm.test.OpenAIIntegrationTest;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Disabled;

@Metadata(
    codemodType = SensitiveDataLoggingCodemod.class,
    testResourceDir = "sensitive-data-logging",
    dependencies = {})
@OpenAIIntegrationTest
@Disabled("codemod is in disrepair")
final class SensitiveDataLoggingCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Remove ALL lines that log sensitive data at INFO or higher severities.
        """;
  }
}
