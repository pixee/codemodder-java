package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = OverridesMatchParentSynchronizationCodemod.class,
    testResourceDir = "overrides-match-synchronization-s3551",
    renameTestFile =
        "auto-delegate-processor/src/main/java/com/ryandens/delegation/AutoDelegateProcessor.java",
    dependencies = {})
final class OverridesMatchParentSynchronizationCodemodTest implements CodemodTestMixin {}
