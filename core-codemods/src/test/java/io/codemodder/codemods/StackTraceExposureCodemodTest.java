package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = StackTraceExposureCodemod.class,
    testResourceDir = "stack-trace-exposure",
    dependencies = {})
final class StackTraceExposureCodemodTest implements CodemodTestMixin {}
