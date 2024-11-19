package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLErrorMessageExposureCodemod.class,
    testResourceDir = "error-message-exposure",
    dependencies = {})
final class CodeQLErrorMessageExposureCodemodTest implements CodemodTestMixin {}
