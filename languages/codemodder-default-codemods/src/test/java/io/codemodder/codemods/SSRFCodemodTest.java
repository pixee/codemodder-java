package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SSRFCodemod.class,
    testResourceDir = "ssrf",
    dependencies = "io.openpixee:java-security-toolkit:1.0.0")
final class SSRFCodemodTest implements CodemodTestMixin {}
