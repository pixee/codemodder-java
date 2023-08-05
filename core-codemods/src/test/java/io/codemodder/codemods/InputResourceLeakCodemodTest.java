package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = InputResourceLeakCodemod.class,
    testResourceDir = "input-resource-leak",
    dependencies = {})
final class InputResourceLeakCodemodTest implements CodemodTestMixin {}
