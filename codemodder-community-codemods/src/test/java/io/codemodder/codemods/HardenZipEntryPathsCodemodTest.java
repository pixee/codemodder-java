package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenZipEntryPathsCodemod.class,
    testResourceDir = "harden-zip-entry-paths",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.6")
final class HardenZipEntryPathsCodemodTest implements CodemodTestMixin {}
