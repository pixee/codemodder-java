package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = AddMissingOverrideCodemod.class,
    testResourceDir = "add-missing-override-s1161",
    renameTestFile = "src/main/java/SqlInjectionLesson10b.java",
    dependencies = {},
    sonarIssuesJsonFiles = {"sonar-issues_1.json", "sonar-issues_2.json"})
final class AddMissingOverrideCodemodTest implements CodemodTestMixin {}
