package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = MigrateSpringJobBuilderFactoryCodemod.class,
    testResourceDir = "migrate-spring-job-builder-factory",
    dependencies = {})
final class MigrateSpringJobBuilderFactoryCodemodTest implements CodemodTestMixin {}
