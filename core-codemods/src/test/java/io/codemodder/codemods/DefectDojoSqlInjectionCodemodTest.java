package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class DefectDojoSqlInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = DefectDojoSqlInjectionCodemod.class,
      testResourceDir = "defectdojo-sql-injection/SqlInjectionChallenge",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionChallenge.java",
      dependencies = {})
  final class DefectDojoSqlInjectionChallengeCodemodTestMixin implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = DefectDojoSqlInjectionCodemod.class,
      testResourceDir = "defectdojo-sql-injection/SqlInjectionLesson8",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/introduction/SqlInjectionLesson8.java",
      dependencies = {})
  final class DefectDojoSqlInjectionLesson8CodemodTestMixin implements CodemodTestMixin {}
}
