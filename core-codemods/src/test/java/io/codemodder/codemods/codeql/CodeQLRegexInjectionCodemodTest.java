package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class CodeQLRegexInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = CodeQLRegexInjectionCodemod.class,
      testResourceDir = "codeql-regex-injection/bannedwordlist",
      renameTestFile = "app/src/main/java/org/apache/roller/weblogger/util/Bannedwordslist.java",
      expectingFixesAtLines = 438,
      doRetransformTest = false,
      dependencies = {})
  final class BannedWordlistTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = CodeQLRegexInjectionCodemod.class,
      testResourceDir = "codeql-regex-injection/regexutil",
      renameTestFile = "app/src/main/java/org/apache/roller/util/RegexUtil.java",
      expectingFixesAtLines = {71, 66, 49},
      doRetransformTest = false,
      dependencies = {})
  final class RegexUtilTest implements CodemodTestMixin {}
}
