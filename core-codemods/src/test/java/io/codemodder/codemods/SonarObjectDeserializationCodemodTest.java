package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SonarObjectDeserializationCodemodTest {

  @Nested
  @Metadata(
      codemodType = SonarObjectDeserializationCodemod.class,
      testResourceDir = "sonar-object-deserialization-s5135",
      renameTestFile =
          "src/main/java/org/owasp/webgoat/lessons/deserialization/InsecureDeserializationTask.java",
      expectingFixesAtLines = {60},
      dependencies = {})
  class ObjectInputStreamCreationTest implements CodemodTestMixin {}
}
