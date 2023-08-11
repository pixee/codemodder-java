package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UpgradeTempFileToNIOCodemod.class,
    testResourceDir = "upgrade-tempfile-to-nio",
    dependencies = {})
final class UpgradeTempFileToNIOCodemodTest implements CodemodTestMixin {}
