package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenZipEntryPathsCodemod.class,
    testResourceDir = "harden-zip-entry-paths",
        dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class HardenZipEntryPathsCodemodTest implements CodemodTestMixin {}
