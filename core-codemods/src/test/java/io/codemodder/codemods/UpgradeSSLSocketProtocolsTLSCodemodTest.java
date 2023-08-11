package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UpgradeSSLSocketProtocolsTLSCodemod.class,
    testResourceDir = "upgrade-sslsocket-tls",
    dependencies = {})
final class UpgradeSSLSocketProtocolsTLSCodemodTest implements CodemodTestMixin {}
