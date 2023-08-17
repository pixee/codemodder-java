package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = VerboseRequestMappingCodemod.class,
    testResourceDir = "verbose-request-mapping",
    dependencies = {})
final class VerboseRequestMappingCodemodTest implements CodemodTestMixin {}
