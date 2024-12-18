package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveUnusedImportCodemod.class,
    testResourceDir = "remove-unused-import-s1128",
    renameTestFile = "sonar/src/main/java/ai/pixee/triage/sonar/SonarTriageProvider.java",
    dependencies = {})
final class RemoveUnusedImportCodemodTest implements CodemodTestMixin {}
