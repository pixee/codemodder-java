package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = LimitReadlineCodemod.class,
    testResourceDir = "limit-readline",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.6")
final class LimitReadlineCodemodTest implements CodemodTestMixin {}
