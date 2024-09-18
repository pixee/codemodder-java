package io.codemodder.codemods.semgrep;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepServletResponseWriterXSSCodemod.class,
    testResourceDir = "semgrep-responsewriter-xss",
    expectingFixesAtLines = {15},
    renameTestFile = "projects/acme/semgrep-responsewriter-xss/Servlets.java",
    dependencies = {})
final class SemgrepServletResponseWriterXSSCodemodTest implements CodemodTestMixin {}
