package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLZipSlipHandler.class,
    testResourceDir = "codeql-zipslip",
    renameTestFile = "Path Traversal/ZipTraversal.java",
    expectingFixesAtLines = {11},
    dependencies = {})
final class CodeQLZipSlipHandlerTest implements CodemodTestMixin {}
