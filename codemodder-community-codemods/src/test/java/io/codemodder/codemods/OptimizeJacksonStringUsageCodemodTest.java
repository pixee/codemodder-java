package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = OptimizeJacksonStringUsageCodemod.class,
    testResourceDir = "optimize-jackson-string-usage",
    dependencies = {})
final class OptimizeJacksonStringUsageCodemodTest implements CodemodTestMixin {}
