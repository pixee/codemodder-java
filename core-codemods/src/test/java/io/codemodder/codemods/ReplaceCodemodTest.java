package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
        codemodType = ReplaceCodemod.class,
        testResourceDir = "replace-s5361",
        renameTestFile = "src/main/java/org/owasp/webgoat/lessons/ssrf/SSRFTask2.java",
        dependencies = {})
public class ReplaceCodemodTest implements CodemodTestMixin {
}
