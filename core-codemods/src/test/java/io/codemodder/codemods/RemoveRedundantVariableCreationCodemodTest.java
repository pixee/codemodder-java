package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveRedundantVariableCreationCodemod.class,
    testResourceDir = "remove-redundant-variable-creation-s1488",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/MvcConfiguration.java",
    dependencies = {})
final class RemoveRedundantVariableCreationCodemodTest implements CodemodTestMixin {}
