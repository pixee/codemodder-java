package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarSSRFCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarSSRFCodemod.class,
      testResourceDir = "sonar-ssrf-s5144/resttemplate",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/passwordreset/ResetLinkAssignmentForgotPassword.java",
      expectingFixesAtLines = {104},
      dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
  final class RestTemplateTest implements CodemodTestMixin {}
}
