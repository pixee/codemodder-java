package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = TextBlockCodemod.class,
    testResourceDir = "text-block-s6126",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/challenges/challenge7/Assignment7.java",
    dependencies = {})
final class TextBlockCodemodTest implements CodemodTestMixin {}
