package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLJDBCResourceLeakCodemod.class,
    testResourceDir = "database-resource-leak",
    dependencies = {})
final class CodeQLJDBCResourceLeakCodemodTest implements CodemodTestMixin {}
