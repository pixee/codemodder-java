package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ReplaceGenericsInVarCodemod.class,
    testResourceDir = "replace-generics-in-var",
    dependencies = {})
final class ReplaceGenericsInVarCodemodTest implements CodemodTestMixin {}
