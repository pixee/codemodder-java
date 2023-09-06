package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.llm.LLMVerifyingCodemodTestMixin;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Have test cases that look like the following:
 *
 * <ol>
 *   <li>an old-school Struts-style controller class lookup based on constant prefix
 *   <li>a class lookup that looks like a JDBC driver reflection using a file-sourced property
 *   <li>an OSGi class loader that looks up classes
 * </ol>
 */
@Execution(ExecutionMode.CONCURRENT)
final class ReflectionInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection/driver_loading",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/RDSUtil.java",
      doRetransformTest = false,
      dependencies = {})
  final class SuppressJdbcDriverFindingTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
              - The Class.forName() call on line 91 has been replaced with a constant variable or literal string
              """;
    }
  }

  @Nested
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection/is_constant_from_method",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/ActionLoader.java",
      doRetransformTest = false,
      dependencies = {})
  final class SuppressConstantButIndirectThroughMethodTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
              - The Class.forName() call on line 34 must be passed a literal string or a constant reference.
              """;
    }
  }

  @Nested
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection/has_constant_prefix",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/ActionLoader.java",
      doRetransformTest = false,
      dependencies = {})
  final class SuppressFindingWithConstantPrefixTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
              - The Class.forName() call on line 34 had a Semgrep suppression comment added above it.
              """;
    }
  }

  /**
   * On top of testing the outcome of "unverifiable_and_potentially_intentionally_unsafe", this
   * codemod also tests that multiple outcomes on different lines can be handled.
   */
  @Nested
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection/unverifiable_and_potentially_intentionally_unsafe",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/Main.java",
      doRetransformTest = false,
      dependencies = {})
  final class MultipleSuppressClassLoaderAndHardenUnknownTest
      implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
              - The Class.forName() call inside loadGameTypeFromPlugin() had a Semgrep suppression comment added above it, and the call itself was left untouched.
              - The Class.forName() call inside loadGameTypeFromRequest() was replaced by loadAndVerify() to add security
              """;
    }
  }

  /**
   * Confirms that other APIs besides Class.forName() will not be handled even if cited by the
   * SARIF.
   */
  @Nested
  @Metadata(
      codemodType = ReflectionInjectionCodemod.class,
      testResourceDir = "reflection-injection/not_class_forname",
      renameTestFile = "acme-webapp/core/src/main/java/com/acme/core/Main.java",
      doRetransformTest = false,
      dependencies = {})
  final class NotClassForNameTest implements LLMVerifyingCodemodTestMixin {
    @Override
    public String getRequirementsPrompt() {
      return """
              - The code has not been modified.
              """;
    }
  }
}
