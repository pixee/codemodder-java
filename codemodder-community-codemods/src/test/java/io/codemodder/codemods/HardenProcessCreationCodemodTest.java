package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenProcessCreationCodemod.class,
    testResourceDir = "harden-process-creation",
    dependencies = {})
final class HardenProcessCreationCodemodTest implements CodemodTestMixin {}
