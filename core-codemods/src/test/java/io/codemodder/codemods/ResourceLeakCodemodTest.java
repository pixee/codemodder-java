package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ResourceLeakCodemod.class,
    testResourceDir = "resource-leak",
    dependencies = {})
final class ResourceLeakCodemodTest implements CodemodTestMixin {}
