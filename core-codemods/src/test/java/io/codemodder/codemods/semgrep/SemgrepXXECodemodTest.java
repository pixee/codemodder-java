package io.codemodder.codemods.semgrep;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepXXECodemod.class,
    testResourceDir = "semgrep-xxe",
    expectingFixesAtLines = {71},
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/sqlinjection/introduction/XXELesson.java",
    dependencies = {})
final class SemgrepXXECodemodTest implements CodemodTestMixin {}
