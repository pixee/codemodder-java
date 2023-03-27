package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = LimitReadlineCodemod.class,
    testResourceDir = "limit-readline",
    dependencies = {})
final class LimitReadlineCodemodTest implements CodemodTestMixin {}
