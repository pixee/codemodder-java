package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ValidateJakartaForwardPathCodemod.class,
    testResourceDir = "validate-jakarta-forward-path",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.6")
final class ValidateJakartaForwardPathCodemodTest implements CodemodTestMixin {}
