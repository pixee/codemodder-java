package io.codemodder.codemods;

import static io.codemodder.plugins.llm.StandardModel.GPT_4;

import io.codemodder.plugins.llm.Model;
import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;
import org.junit.jupiter.api.condition.EnabledIf;

@Metadata(
    codemodType = LogFailedLoginCodemod.class,
    testResourceDir = "log-failed-login",
    dependencies = {})
@EnabledIf("io.codemodder.testutils.llm.CodemodderOpenAIKeys#isAvailable")
public final class LogFailedLoginCodemodTest implements LLMVerifyingCodemodTestMixin {

  @Override
  public String getRequirementsPrompt() {
    return """
        - Log a message when a login attempt fails.
        - Use the same log message style as the rest of the code.
        """;
  }

  /**
   * Use GPT-4, because the codemod itself also uses GPT-4, and the verification logic was
   * erroneously failing with GPT-3.5.
   */
  @Override
  public Model model() {
    return GPT_4;
  }
}
