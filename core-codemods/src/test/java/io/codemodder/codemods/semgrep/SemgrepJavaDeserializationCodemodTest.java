package io.codemodder.codemods.semgrep;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepJavaDeserializationCodemod.class,
    testResourceDir = "semgrep-java-deserialization",
    expectingFixesAtLines = {57},
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/deserialization/InsecureDeserializationTask.java",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SemgrepJavaDeserializationCodemodTest implements CodemodTestMixin {}
