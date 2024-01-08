package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DeclareVariableOnSeparateLineCodemod.class,
    testResourceDir = "declare-variable-on-separate-line-s1659",
    renameTestFile =
        "src/main/java/org/owasp/webgoat/container/assignments/AssignmentEndpoint.java",
    dependencies = {})
final class DeclareVariableOnSeparateLineCodemodTest implements CodemodTestMixin {}
