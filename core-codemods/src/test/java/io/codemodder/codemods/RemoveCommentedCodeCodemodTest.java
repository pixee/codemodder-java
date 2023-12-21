package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveCommentedCodeCodemod.class,
    testResourceDir = "remove-commented-code-s125",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/assignments/AttackResult.java",
    dependencies = {})
final class RemoveCommentedCodeCodemodTest implements CodemodTestMixin {}
