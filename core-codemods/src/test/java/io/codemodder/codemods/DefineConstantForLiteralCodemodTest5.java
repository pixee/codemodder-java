package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192/case-5",
    renameTestFile = "codegen/src/main/java/software/amazon/awssdk/codegen/docs/WaiterDocs.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest5 implements CodemodTestMixin {}
