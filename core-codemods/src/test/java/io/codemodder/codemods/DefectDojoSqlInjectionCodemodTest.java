package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class DefectDojoSqlInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = DefectDojoSqlInjectionCodemod.class,
      testResourceDir = "defectdojo-sql-injection/SqlInjectionChallenge",
      doRetransformTest = false,
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/advanced/SqlInjectionChallenge.java",
      dependencies = {},
      expectingFixesAtLines = {69})
  final class WebGoatSqlInjectionChallengeTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = DefectDojoSqlInjectionCodemod.class,
      testResourceDir = "defectdojo-sql-injection/SqlInjectionLesson8",
      doRetransformTest = false,
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/introduction/SqlInjectionLesson8.java",
      dependencies = {},
      expectingFixesAtLines = {78, 158})
  final class WebGoatSqlInjectionLesson8Test implements CodemodTestMixin {}
}
