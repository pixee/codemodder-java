package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class SSRFTest {

  @Test
  void it_protects_ssrf() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSRFVulnerability.java");
  }
}
