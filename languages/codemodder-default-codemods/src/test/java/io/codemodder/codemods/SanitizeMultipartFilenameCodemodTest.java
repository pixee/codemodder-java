package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeMultipartFilenameCodemod.class,
    testResourceDir = "sanitize-multipart-filename",
    dependencies = "io.openpixee:java-security-toolkit:1.0.0")
final class SanitizeMultipartFilenameCodemodTest implements CodemodTestMixin {}
