package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLOutputResourceLeakCodemod.class,
    testResourceDir = "output-resource-leak",
    dependencies = {})
final class CodeQLOutputResourceLeakCodemodTest implements CodemodTestMixin {}
