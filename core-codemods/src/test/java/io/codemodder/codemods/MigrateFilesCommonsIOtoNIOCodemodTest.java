package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = MigrateFilesCommonsIOToNIOCodemod.class,
    testResourceDir = "migrate-files-commons-io-to-nio",
    dependencies = {})
final class MigrateFilesCommonsIOtoNIOCodemodTest implements CodemodTestMixin {}
