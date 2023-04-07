package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UpgradeSSLParametersTLSCodemod.class,
    testResourceDir = "upgrade-sslparameters-tls",
    dependencies = {})
final class UpgradeSSLParametersTLSCodemodTest implements CodemodTestMixin {}
