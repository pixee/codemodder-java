package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarSQLInjectionCodemodsTest {

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionHotspotCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/unsupported",
      renameTestFile = "src/main/java/org/owasp/webgoat/container/users/UserService.java",
      expectingFailedFixesAtLines = {52}, // we don't support this method
      dependencies = {})
  class UnsupportedTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionIssueCodemod.class,
      testResourceDir = "sonar-sql-injection-s3649",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionChallenge.java",
      expectingFixesAtLines = {69},
      dependencies = {})
  class FromIssueRatherThanHotspotTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionHotspotCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/supported",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionChallenge.java",
      expectingFixesAtLines = {69},
      dependencies = {})
  class SupportedHotspotTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionHotspotCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/supportedTableInjection",
      renameTestFile = "core-codemods/src/main/java/io/codemodder/codemods/SQLTest.java",
      expectingFixesAtLines = {19, 25, 33, 40},
      dependencies = {})
  class SupportedTableInjectionTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SonarSQLInjectionHotspotCodemod.class,
      testResourceDir = "sonar-sql-injection-s2077/supportedMixedInjections",
      renameTestFile = "core-codemods/src/main/java/io/codemodder/codemods/SQLTestMixed.java",
      expectingFixesAtLines = {21},
      dependencies = {})
  class SupportedMixedInjectionTest implements CodemodTestMixin {}
}
