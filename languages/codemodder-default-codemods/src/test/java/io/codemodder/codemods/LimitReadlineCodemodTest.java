package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = LimitReadlineCodemod.class,
    testResourceDir = "limit-readline",
    dependencies = "io.openpixee:java-security-toolkit:1.0.0")
final class LimitReadlineCodemodTest implements CodemodTestMixin {}
