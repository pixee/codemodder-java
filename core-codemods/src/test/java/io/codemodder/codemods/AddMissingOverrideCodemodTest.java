package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = AddMissingOverrideCodemod.class,
    testResourceDir = "add-missing-override-s1161",
    renameTestFile = "src/main/java/SqlInjectionLesson10b.java",
    dependencies = {})
final class AddMissingOverrideCodemodTest implements CodemodTestMixin {}
