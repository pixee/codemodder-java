package io.codemodder.codemods.semgrep;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepMissingSecureFlagCodemod.class,
    testResourceDir = "semgrep-missing-secure-flag",
    expectingFixesAtLines = {131},
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/jwt/JWTVotesEndpoint.java",
    doRetransformTest = false,
    dependencies = {})
final class SemgrepMissingSecureFlagCodemodTest implements CodemodTestMixin {}
