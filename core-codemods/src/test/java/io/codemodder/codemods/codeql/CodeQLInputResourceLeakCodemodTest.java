package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLInputResourceLeakCodemod.class,
    testResourceDir = "input-resource-leak",
    dependencies = {})
final class CodeQLInputResourceLeakCodemodTest implements CodemodTestMixin {}
