package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class HeaderInjectionTest {

  @Test
  void it_prevents_header_injection() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/HeaderInjectionVulnerability.java",
        new HeaderInjectionVisitorFactoryNg());
  }
}
