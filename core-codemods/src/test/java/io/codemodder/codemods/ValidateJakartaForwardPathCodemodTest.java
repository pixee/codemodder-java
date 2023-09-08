package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ValidateJakartaForwardPathCodemod.class,
    testResourceDir = "validate-jakarta-forward-path",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class ValidateJakartaForwardPathCodemodTest implements CodemodTestMixin {}
