package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RandomizeSeedCodemod.class,
    testResourceDir = "make-prng-seed-unpredictable",
    dependencies = {})
final class RandomizeSeedCodemodTest implements CodemodTestMixin {}
