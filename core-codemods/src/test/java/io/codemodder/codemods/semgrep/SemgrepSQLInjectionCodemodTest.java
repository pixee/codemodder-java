package io.codemodder.codemods.semgrep;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SemgrepSQLInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = SemgrepSQLInjectionFormattedSqlStringCodemod.class,
      testResourceDir = "semgrep-sql-injection-formatted-sql-string",
      expectingFixesAtLines = {67},
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/introduction/SqlInjectionLesson5a.java",
      dependencies = {})
  final class SemgrepSQLInjectionFormattedSqlStringCodemodTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SemgrepSQLInjectionCodemod.class,
      testResourceDir = "semgrep-sql-injection",
      expectingFixesAtLines = {78},
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/sqlinjection/introduction/SqlInjectionLesson8.java",
      dependencies = {})
  final class SemgrepSQLInjectionJdbcCodemodTest implements CodemodTestMixin {}
}
