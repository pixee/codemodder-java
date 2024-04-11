package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SemgrepOverlyPermissiveFilePermissionsCodemod.class,
    testResourceDir = "semgrep-overly-permissive-file-permission",
    doRetransformTest = false,
    dependencies = {},
    expectingFixesAtLines = {14, 15, 25, 22})
final class SemgrepOverlyPermissiveFilePermissionsCodemodTest implements CodemodTestMixin {}
