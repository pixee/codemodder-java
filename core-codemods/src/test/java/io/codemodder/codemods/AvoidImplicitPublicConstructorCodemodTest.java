package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = AvoidImplicitPublicConstructorCodemod.class,
    testResourceDir = "avoid-implicit-public-constructor-s1118",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/cryptography/CryptoUtil.java",
    dependencies = {})
final class AvoidImplicitPublicConstructorCodemodTest implements CodemodTestMixin {}
