package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = FixRedundantStaticOnEnumCodemod.class,
    testResourceDir = "remove-redundant-static-s2786",
    dependencies = {})
final class FixRedundantStaticOnEnumCodemodTest implements CodemodTestMixin {}
