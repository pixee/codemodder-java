package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveCommentedCodeCodemod.class,
    testResourceDir = "remove-commented-code-s125",
    renameTestFile =
        "core-codemods/src/main/java/io/codemodder/codemods/HQLParameterizationCodemod.java",
    dependencies = {})
final class RemoveCommentedCodeCodemodTest implements CodemodTestMixin {}
