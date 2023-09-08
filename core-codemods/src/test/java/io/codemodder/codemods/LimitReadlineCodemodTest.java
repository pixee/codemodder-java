package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = LimitReadlineCodemod.class,
    testResourceDir = "limit-readline",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class LimitReadlineCodemodTest implements CodemodTestMixin {}
