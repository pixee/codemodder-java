package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = JEXLInjectionCodemod.class,
    testResourceDir = "jexl-expression-injection",
    dependencies = {})
final class JEXLInjectionCodemodTest implements CodemodTestMixin {}
