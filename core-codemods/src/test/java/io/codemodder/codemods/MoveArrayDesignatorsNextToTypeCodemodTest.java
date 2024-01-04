package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = MoveArrayDesignatorsNextToTypeCodemod.class,
    testResourceDir = "move-array-designators-next-to-type-s1197",
    renameTestFile = "src/main/java/org/owasp/webgoat/lessons/challenges/challenge7/MD5.java",
    dependencies = {})
final class MoveArrayDesignatorsNextToTypeCodemodTest implements CodemodTestMixin {}
