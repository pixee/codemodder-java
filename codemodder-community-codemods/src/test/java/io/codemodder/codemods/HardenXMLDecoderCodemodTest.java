package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenXMLDecoderCodemod.class,
    testResourceDir = "harden-xmldecoder-stream",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.3")
final class HardenXMLDecoderCodemodTest implements CodemodTestMixin {}
