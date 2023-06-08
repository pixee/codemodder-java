package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SecureRandomCodemod.class,
    testResourceDir = "secure-random",
    dependencies = {})
final class SecureRandomCodemodTest implements CodemodTestMixin {}
