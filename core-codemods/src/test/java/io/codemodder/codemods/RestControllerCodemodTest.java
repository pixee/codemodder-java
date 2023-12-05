package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RestControllerCodemod.class,
    testResourceDir = "rest-controller-s6833",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/service/SessionService.java",
    dependencies = {})
final class RestControllerCodemodTest implements CodemodTestMixin {}
