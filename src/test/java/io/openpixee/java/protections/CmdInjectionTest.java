package io.openpixee.java.protections;

import static io.openpixee.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class CmdInjectionTest {

  @Test
  void it_prevents_cmd_injection() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/CmdInjectionVulnerability.java",
        new RuntimeExecVisitorFactory());
  }
}
