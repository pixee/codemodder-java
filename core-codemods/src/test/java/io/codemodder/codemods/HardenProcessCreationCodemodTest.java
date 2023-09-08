package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenProcessCreationCodemod.class,
    testResourceDir = "harden-process-creation",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class HardenProcessCreationCodemodTest implements CodemodTestMixin {}
