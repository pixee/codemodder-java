package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLUnverifiedJwtCodemod.class,
    testResourceDir = "missing-jwt-signature-check",
    dependencies = {})
final class CodeQLUnverifiedJwtCodemodTest implements CodemodTestMixin {}
