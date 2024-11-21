package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import org.junit.jupiter.api.Nested;

final class CodeQLErrorMessageExposureCodemodTest {

  @Nested
  @Metadata(
      codemodType = CodeQLErrorMessageExposureCodemod.class,
      testResourceDir = "error-message-exposure/IntoPrinter",
      renameTestFile =
          "app/src/main/java/org/apache/roller/weblogger/ui/rendering/servlets/TrackbackServlet"
              + ".java",
      expectingFixesAtLines = {147, 221},
      dependencies = {})
  final class IntoPrinterTest implements CodemodTestMixin {}

  @Nested
  @Metadata(
      codemodType = CodeQLErrorMessageExposureCodemod.class,
      testResourceDir = "error-message-exposure/SendErrorAndPrintStackTrace",
      dependencies = {})
  final class SendErrorAndPrintStackTraceTest implements CodemodTestMixin {}
}
