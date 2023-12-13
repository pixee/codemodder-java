package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192",
    renameTestFile = "core-codemods/src/main/java/io/codemodder/codemods/JEXLInjectionCodemod.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest implements CodemodTestMixin {}
