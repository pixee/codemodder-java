package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLJEXLInjectionCodemod.class,
    testResourceDir = "jexl-expression-injection",
    doRetransformTest = false,
    dependencies = {})
final class CodeQLJEXLInjectionCodemodTest implements CodemodTestMixin {}
