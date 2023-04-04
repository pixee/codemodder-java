package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SQLParameterizerCodemod.class,
    testResourceDir = "sql-parameterizer",
    dependencies = {})
final class SQLParameterizerCodemodTest implements CodemodTestMixin {}
