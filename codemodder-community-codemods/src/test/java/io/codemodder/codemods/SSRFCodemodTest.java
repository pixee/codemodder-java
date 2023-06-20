package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SSRFCodemod.class,
    testResourceDir = "ssrf",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.4")
final class SSRFCodemodTest implements CodemodTestMixin {}
