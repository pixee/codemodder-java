package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192/case-3",
    renameTestFile =
        "codegen-lite/src/main/java/software/amazon/awssdk/codegen/lite/regions/PartitionMetadataGenerator.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest3 implements CodemodTestMixin {}
