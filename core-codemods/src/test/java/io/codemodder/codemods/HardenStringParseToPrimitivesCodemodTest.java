package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenStringParseToPrimitivesCodemod.class,
    testResourceDir = "harden-string-parse-to-primitives-s2130",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/challenges/Flags.java",
    dependencies = {})
final class HardenStringParseToPrimitivesCodemodTest implements CodemodTestMixin {}
