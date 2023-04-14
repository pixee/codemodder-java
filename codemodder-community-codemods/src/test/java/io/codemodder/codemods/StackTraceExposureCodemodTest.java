package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = StackTraceExposureCodemod.class,
    testResourceDir = "stack-trace-exposure",
    dependencies = {})
public class StackTraceExposureCodemodTest implements CodemodTestMixin {}
