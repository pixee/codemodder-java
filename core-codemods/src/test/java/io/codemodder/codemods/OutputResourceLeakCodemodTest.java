package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = OutputResourceLeakCodemod.class,
    testResourceDir = "output-resource-leak",
    dependencies = {})
final class OutputResourceLeakCodemodTest implements CodemodTestMixin {}
