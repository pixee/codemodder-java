package io.codemodder.examples;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = MakeJUnit5TestsPackagePrivateCodemod.class,
    testResourceDir = "make-junit5-tests-package-private",
    dependencies = {})
final class MakeJUnit5TestsPackagePrivateCodemodTest implements CodemodTestMixin {}
