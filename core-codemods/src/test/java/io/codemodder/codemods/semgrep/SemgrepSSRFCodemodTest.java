package io.codemodder.codemods.semgrep;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepSSRFCodemod.class,
    testResourceDir = "semgrep-ssrf",
    expectingFixesAtLines = {51},
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/jwt/claimmisuse/JWTHeaderJKUEndpoint.java",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SemgrepSSRFCodemodTest implements CodemodTestMixin {}
