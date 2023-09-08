package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenXMLDecoderCodemod.class,
    testResourceDir = "harden-xmldecoder-stream",
        dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class HardenXMLDecoderCodemodTest implements CodemodTestMixin {}
