package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

/** Test for {@link SonarRemoveUnthrowableExceptionCodemod}. */
@Metadata(
    codemodType = SonarRemoveUnthrowableExceptionCodemod.class,
    testResourceDir = "remove-unthrowable-s1130",
    renameTestFile = "src/main/ThingTest.java",
    expectingFixesAtLines = {24},
    doRetransformTest = false,
    dependencies = {})
final class SonarRemoveUnthrowableCodemodTest implements CodemodTestMixin {}
