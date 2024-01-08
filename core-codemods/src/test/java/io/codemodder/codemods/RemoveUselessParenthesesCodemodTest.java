package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveUselessParenthesesCodemod.class,
    testResourceDir = "remove-useless-parentheses-s1110",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/challenges/challenge7/MD5.java",
    dependencies = {})
final class RemoveUselessParenthesesCodemodTest implements CodemodTestMixin {}
