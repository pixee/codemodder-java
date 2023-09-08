package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenJavaDeserializationCodemod.class,
    testResourceDir = "harden-java-deserialization",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class HardenJavaDeserializationCodemodTest implements CodemodTestMixin {}
