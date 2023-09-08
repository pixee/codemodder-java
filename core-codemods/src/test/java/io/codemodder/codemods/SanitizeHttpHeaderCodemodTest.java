package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeHttpHeaderCodemod.class,
    testResourceDir = "strip-http-header-newlines",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SanitizeHttpHeaderCodemodTest implements CodemodTestMixin {}
