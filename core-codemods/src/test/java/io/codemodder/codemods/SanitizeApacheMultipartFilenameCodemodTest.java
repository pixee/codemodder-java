package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeApacheMultipartFilenameCodemod.class,
    testResourceDir = "sanitize-apache-multipart-filename",
    dependencies = "io.github.pixee:java-security-toolkit:1.0.7")
final class SanitizeApacheMultipartFilenameCodemodTest implements CodemodTestMixin {}
