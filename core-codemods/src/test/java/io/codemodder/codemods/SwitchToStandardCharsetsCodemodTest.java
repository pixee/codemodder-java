package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SwitchToStandardCharsetsCodemod.class,
    testResourceDir = "switch-to-standard-charsets",
    dependencies = {})
final class SwitchToStandardCharsetsCodemodTest implements CodemodTestMixin {}
