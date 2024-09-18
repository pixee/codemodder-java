package io.codemodder.codemods.semgrep;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepWeakRandomCodemod.class,
    testResourceDir = "semgrep-weak-random",
    expectingFixesAtLines = {17},
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/challenges/challenge1/ImageServlet.java",
    dependencies = {})
final class SemgrepWeakRandomCodemodTest implements CodemodTestMixin {}
