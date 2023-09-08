package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeApacheMultipartFilenameCodemod.class,
    testResourceDir = "sanitize-apache-multipart-filename",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SanitizeApacheMultipartFilenameCodemodTest implements CodemodTestMixin {}
