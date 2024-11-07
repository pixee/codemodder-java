package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class SQLParameterizerCodemodTest {

  @Nested
  @Metadata(
      codemodType = SQLParameterizerCodemod.class,
      testResourceDir = "sql-parameterizer/defaultTransformation",
      dependencies = {})
  class DefaultTransformationTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = SQLParameterizerCodemod.class,
      testResourceDir = "sql-parameterizer/hijackTransformation",
      dependencies = {})
  class HijackTransformationTest implements CodemodTestMixin {}
}
