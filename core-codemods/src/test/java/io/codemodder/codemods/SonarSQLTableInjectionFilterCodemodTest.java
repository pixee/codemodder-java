package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarSQLTableInjectionFilterCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarSQLTableInjectionFilterCodemod.class,
      testResourceDir = "sonar-sql-table-injection-filter-s2077/supported",
      renameTestFile = "core-codemods/src/main/java/io/codemodder/codemods/SQLTest.java",
      expectingFixesAtLines = {18},
      dependencies = {})
  class SupportedTest implements CodemodTestMixin {}
}
