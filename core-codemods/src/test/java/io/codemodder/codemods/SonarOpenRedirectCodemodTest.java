package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarOpenRedirectCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarOpenRedirectCodemod.class,
      testResourceDir = "sonar-open-redirect-s5146",
      renameTestFile = "src/main/java/com/mycompany/app/RedirectionForgery.java",
      expectingFixesAtLines = {8},
      dependencies = {})
  class ResponseServletRedirectTest implements CodemodTestMixin {}
}
