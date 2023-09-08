package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SanitizeSpringMultipartFilenameCodemod.class,
    testResourceDir = "sanitize-spring-multipart-filename",
        dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class SanitizeSpringMultipartFilenameCodemodTest implements CodemodTestMixin {}
