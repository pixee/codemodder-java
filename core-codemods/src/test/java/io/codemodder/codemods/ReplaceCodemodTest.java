package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
        codemodType = ReplaceCodemod.class,
        testResourceDir = "replace-s5361",
        dependencies = {})
public class ReplaceCodemodTest implements CodemodTestMixin {
}
