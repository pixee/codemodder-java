package io.codemodder.codemods.semgrep;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepReflectionInjectionCodemod.class,
    testResourceDir = "semgrep-reflection-injection",
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/xxe/introduction/TranslationController.java",
    expectingFixesAtLines = {83},
    doRetransformTest = false,
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SemgrepReflectionInjectionCodemodTest implements CodemodTestMixin {}
