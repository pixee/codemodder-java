package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;
import org.junit.jupiter.api.condition.EnabledIf;

@Execution(ExecutionMode.SAME_THREAD)
@Metadata(
    codemodType = SensitiveDataLoggingCodemod.class,
    testResourceDir = "sensitive-data-logging",
    dependencies = {})
@EnabledIf("io.codemodder.testutils.llm.CodemodderOpenAIKeys#isAvailable")
final class SensitiveDataLoggingCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Remove ALL lines that log sensitive data at INFO or higher severities.
        """;
  }
}
