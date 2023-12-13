package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveUnusedLocalVariableCodemod.class,
    testResourceDir = "remove-unused-local-variable-s1481",
    renameTestFile =
        "src/main/java/org/owasp/webgoat/container/assignments/AssignmentEndpoint.java",
    dependencies = {})
final class RemoveUnusedLocalVariableCodemodTest implements CodemodTestMixin {}
