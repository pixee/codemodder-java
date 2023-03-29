package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class XXETest {

  @Test
  void it_prevents_xxe() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/XXEVulnerability.java", new XXEVisitorFactory());
  }
}
