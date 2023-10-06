package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = FixUnsafeNIOPathComparisonCodemod.class,
    testResourceDir = "fix-unsafe-nio-path-comparison",
    dependencies = {})
final class FixUnsafeNIOPathComparisonCodemodTest implements CodemodTestMixin {}
