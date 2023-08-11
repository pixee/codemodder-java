package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UpgradeSSLContextTLSCodemod.class,
    testResourceDir = "upgrade-sslcontext-tls",
    dependencies = {})
final class UpgradeSSLContextTLSCodemodTest implements CodemodTestMixin {}
