package io.codemodder.codemods.sonar;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarWeakHashingAlgorithmCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarWeakHashingAlgorithmCodemod.class,
      testResourceDir = "sonar-weak-hash-4790",
      renameTestFile = "src/main/java/org/owasp/webgoat/lessons/cryptography/HashingAssignment.java",
      expectingFixesAtLines = {55},
      doRetransformTest = false,
      dependencies = {})
  final class HashingAssignmentTest implements CodemodTestMixin {}
}
