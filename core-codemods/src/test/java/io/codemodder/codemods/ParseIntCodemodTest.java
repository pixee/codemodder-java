package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ParseIntCodemod.class,
    testResourceDir = "parse-int-s2130",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/challenges/Flags.java",
    dependencies = {})
final class ParseIntCodemodTest implements CodemodTestMixin {}
