package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;
import org.junit.jupiter.api.Disabled;

/**
 * Have test cases that look like the following:
 *
 * <ol>
 *   <li>an old-school Struts-style controller class lookup based on constant prefix
 *   <li>a class lookup that looks like a JDBC driver reflection using a file-sourced property
 *   <li>an OSGi class loader that looks up classes
 * </ol>
 */
final class ReflectionInjectionCodemodTest {

  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection-no-dependency-injection",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/RDSUtil.java",
      dependencies = {})
  static final class DoesntInjectControlTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
                    - No user input is used to load classes.
                    """;
    }
  }

  @Disabled
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection-with-dependency-injection",
      dependencies = "io.github.pixee:java-security-toolkit:1.0.6")
  static final class InjectsControlTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
                    - No user input is used to load classes without verification.
                    """;
    }
  }
}
