package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = FixRedundantStaticOnEnumCodemod.class,
    testResourceDir = "remove-redundant-static-s2786",
    renameTestFile = "src/main/java/Response.java",
    dependencies = {})
final class FixRedundantStaticOnEnumCodemodTest implements CodemodTestMixin {}
