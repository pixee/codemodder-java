package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLRegexDoSCodemod.class,
    testResourceDir = "codeql-regexdos",
    renameTestFile = "app/src/main/java/org/apache/roller/util/RegexUtil.java",
    expectingFixesAtLines = {62},
    dependencies = {})
final class CodeQLRegexDoSCodemodTest implements CodemodTestMixin {}
