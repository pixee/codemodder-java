package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SwitchLiteralFirstComparisonsCodemod.class,
    testResourceDir = "switch-literal-first-comparisons",
    dependencies = {})
final class SwitchLiteralFirstComparisonsCodemodTest implements CodemodTestMixin {}
