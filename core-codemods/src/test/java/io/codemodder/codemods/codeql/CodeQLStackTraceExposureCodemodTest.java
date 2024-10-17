package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLStackTraceExposureCodemod.class,
    testResourceDir = "stack-trace-exposure",
    dependencies = {})
final class CodeQLStackTraceExposureCodemodTest implements CodemodTestMixin {}
