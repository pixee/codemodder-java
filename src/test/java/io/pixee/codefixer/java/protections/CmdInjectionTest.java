package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class CmdInjectionTest {

  @Test
  void it_prevents_cmd_injection() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/CmdInjectionVulnerability.java",
        new RuntimeExecVisitorFactoryNg());
  }
}
