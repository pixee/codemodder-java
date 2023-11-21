package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = AddMissingOverrideCodemod.class,
    testResourceDir = "add-missing-override-s1161",
    dependencies = {})
final class AddMissingOverrideCodemodTest implements CodemodTestMixin {}
