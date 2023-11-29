package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SubstituteReplaceAllCodemod.class,
    testResourceDir = "substitute-replaceAll-s5361",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/ssrf/SSRFTask2.java",
    dependencies = {})
final class SubstituteReplaceAllCodemodTest implements CodemodTestMixin {}
