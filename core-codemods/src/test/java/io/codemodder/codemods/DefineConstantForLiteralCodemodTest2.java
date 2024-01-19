package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192/case-2",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/idor/IDORLogin.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest2 implements CodemodTestMixin {}
