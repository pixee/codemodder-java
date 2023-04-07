package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UpgradeSSLEngineTLSCodemod.class,
    testResourceDir = "upgrade-sslengine-tls",
    dependencies = {})
final class UpgradeSSLEngineTLSCodemodTest implements CodemodTestMixin {}
