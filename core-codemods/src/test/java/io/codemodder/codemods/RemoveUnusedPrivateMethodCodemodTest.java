package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveUnusedPrivateMethodCodemod.class,
    testResourceDir = "remove-unused-private-method-s1144",
    renameTestFile =
        "src/main/java/org/owasp/webgoat/lessons/authbypass/AccountVerificationHelper.java",
    dependencies = {})
final class RemoveUnusedPrivateMethodCodemodTest implements CodemodTestMixin {}
