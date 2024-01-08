package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ReplaceStreamCollectorsToListCodemod.class,
    testResourceDir = "replace-collectors-toList-s6204",
    renameTestFile =
        "core-codemods/src/main/java/io/codemodder/codemods/MavenSecureURLCodemod.java",
    dependencies = {})
final class ReplaceStreamCollectorsToListCodemodTest implements CodemodTestMixin {}
