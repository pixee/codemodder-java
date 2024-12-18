package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SonarXXECodemod.class,
    testResourceDir = "sonar-xxe-s2755",
    renameTestFile = "src/main/java/com/acme/XXEVuln.java",
    expectingFixesAtLines = {53, 62, 68},
    dependencies = {})
final class SonarXXECodemodTest implements CodemodTestMixin {}
