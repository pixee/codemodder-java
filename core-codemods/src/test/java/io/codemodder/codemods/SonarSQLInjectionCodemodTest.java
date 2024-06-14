package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SonarSQLInjectionCodemod.class,
    testResourceDir = "sonar-sql-injection-s2077",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/users/UserService.java",
    dependencies = {})
final class SonarSQLInjectionCodemodTest implements CodemodTestMixin {}
