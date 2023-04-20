package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = JDBCResourceLeakCodemod.class,
    testResourceDir = "database-resource-leak",
    dependencies = {})
public class JDBCResourceLeakCodemodTest implements CodemodTestMixin {}
