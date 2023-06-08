package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenProcessCreationCodemod.class,
    testResourceDir = "harden-process-creation",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.3")
final class HardenProcessCreationCodemodTest implements CodemodTestMixin {}
