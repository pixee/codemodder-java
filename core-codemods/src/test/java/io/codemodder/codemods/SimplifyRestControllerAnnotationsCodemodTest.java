package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = SimplifyRestControllerAnnotationsCodemod.class,
    testResourceDir = "simplify-rest-controller-annotations-s6833",
    renameTestFile = "src/main/java/org/owasp/webgoat/container/service/SessionService.java",
    dependencies = {})
final class SimplifyRestControllerAnnotationsCodemodTest implements CodemodTestMixin {}
