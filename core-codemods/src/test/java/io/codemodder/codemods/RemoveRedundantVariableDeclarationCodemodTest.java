package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveRedundantVariableDeclarationCodemod.class,
    testResourceDir = "remove-redundant-variable-declaration-s1488",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/MvcConfiguration.java",
    dependencies = {})
final class RemoveRedundantVariableDeclarationCodemodTest implements CodemodTestMixin {}
