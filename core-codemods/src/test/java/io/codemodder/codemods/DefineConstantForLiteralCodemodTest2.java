package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

/**
 * This test a case where we expect to: 1. Create a new camel case constant taking other constants
 * naming convention as reference 2. Avoid expected collision by adding a counter suffix to new
 * constant
 */
@Metadata(
    codemodType = DefineConstantForLiteralCodemod.class,
    testResourceDir = "define-constant-for-duplicate-literal-s1192-2",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/idor/IDORLogin.java",
    dependencies = {})
final class DefineConstantForLiteralCodemodTest2 implements CodemodTestMixin {}
