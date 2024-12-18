package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarCookieMissingSecureFlagCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarCookieMissingSecureFlagCodemod.class,
      testResourceDir = "sonar-missing-secure-flag-2092",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/spoofcookie/SpoofCookieAssignment.java",
      expectingFixesAtLines = {76},
      doRetransformTest = false,
      dependencies = {})
  final class SpoofCookieAssignmentTest implements CodemodTestMixin {}
}
