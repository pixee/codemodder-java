package io.codemodder.examples;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = MakeJUnit5TestsFinalCodemod.class,
    testResourceDir = "make-junit5-tests-final",
    dependencies = {})
final class MakeJUnit5TestsFinalCodemodTest implements CodemodTestMixin {}
