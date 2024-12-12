package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class CodeQLLogInjectionCodemodTest {

  @Nested
  @Metadata(
      codemodType = CodeQLLogInjectionCodemod.class,
      testResourceDir = "codeql-log-injection/template",
      renameTestFile =
          "app/src/main/java/org/apache/roller/weblogger/ui/struts2/editor/Templates.java",
      doRetransformTest = false,
      expectingFixesAtLines = {124},
      dependencies = {})
  class TemplateTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = CodeQLLogInjectionCodemod.class,
      testResourceDir = "codeql-log-injection/templateedit",
      renameTestFile =
          "app/src/main/java/org/apache/roller/weblogger/ui/struts2/editor/TemplateEdit.java",
      doRetransformTest = false,
      expectingFixesAtLines = {128},
      dependencies = {})
  class TemplateEditTest implements CodemodTestMixin {}
}
