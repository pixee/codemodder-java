package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class JakartaForwardTest {

  @Test
  void it_prevents_unsafe_forward() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactory());
  }
}
