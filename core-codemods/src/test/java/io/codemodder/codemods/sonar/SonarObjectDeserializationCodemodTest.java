package io.codemodder.codemods.sonar;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SonarObjectDeserializationCodemod.class,
    testResourceDir = "sonar-object-deserialization-s5135",
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/deserialization/InsecureDeserializationTask.java",
    expectingFixesAtLines = {60},
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SonarObjectDeserializationCodemodTest implements CodemodTestMixin {}
