package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarSQLInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/unsupported",
      renameTestFile = "src/main/java/org/owasp/webgoat/container/users/UserService.java",
      expectingFailedFixesAtLines = {52}, // we don't support this method
      dependencies = {})
  class UnsupportedTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/supported",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionChallenge.java",
      expectingFixesAtLines = {69},
      dependencies = {})
  class SupportedTest implements CodemodTestMixin {}
}