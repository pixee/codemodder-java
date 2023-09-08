package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenJavaDeserializationCodemod.class,
    testResourceDir = "harden-java-deserialization",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.7")
final class HardenJavaDeserializationCodemodTest implements CodemodTestMixin {}
