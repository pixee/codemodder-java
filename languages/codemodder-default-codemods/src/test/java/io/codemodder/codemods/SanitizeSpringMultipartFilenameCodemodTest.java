package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeSpringMultipartFilenameCodemod.class,
    testResourceDir = "sanitize-spring-multipart-filename",
    dependencies = "io.openpixee:java-security-toolkit:1.0.0")
final class SanitizeSpringMultipartFilenameCodemodTest implements CodemodTestMixin {}
