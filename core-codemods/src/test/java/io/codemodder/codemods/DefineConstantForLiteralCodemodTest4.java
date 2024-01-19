package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192/case-4",
    renameTestFile =
        "codegen-lite/src/main/java/software/amazon/awssdk/codegen/lite/regions/ServiceMetadataGenerator.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest4 implements CodemodTestMixin {}
