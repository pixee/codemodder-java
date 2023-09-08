package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SSRFCodemod.class,
    testResourceDir = "ssrf",
        dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SSRFCodemodTest implements CodemodTestMixin {}
