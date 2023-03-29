package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DisableAutomaticDirContextDeserializationCodemod.class,
    testResourceDir = "disable-dircontext-deserialization",
    dependencies = {})
final class DisableAutomaticDirContextDeserializationCodemodTest implements CodemodTestMixin {}
