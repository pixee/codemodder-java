package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SQLTableInjectionFilterCodemod.class,
    testResourceDir = "sql-table-injection-filter",
    dependencies = {})
final class SQLTableInjectionFilterCodemodTest implements CodemodTestMixin {}
