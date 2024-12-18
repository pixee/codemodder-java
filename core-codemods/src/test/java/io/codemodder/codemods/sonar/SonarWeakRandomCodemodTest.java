package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarWeakRandomCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarWeakRandomCodemod.class,
      testResourceDir = "sonar-weak-prng-2245",
      renameTestFile = "src/main/java/org/owasp/webgoat/lessons/csrf/CSRFGetFlag.java",
      expectingFixesAtLines = {59},
      doRetransformTest = false,
      dependencies = {})
  final class CSRFGetFlagTest implements CodemodTestMixin {}
}
