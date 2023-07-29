package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = UseEmptyForToArrayCodemod.class,
    testResourceDir = "use-empty-for-toarray",
    dependencies = {})
final class UseEmptyForToArrayCodemodTest implements CodemodTestMixin {}
