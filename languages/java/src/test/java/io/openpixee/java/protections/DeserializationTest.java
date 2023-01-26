package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class DeserializationTest {

  @Test
  void it_prevents_deserialization() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/DeserializationVulnerability.java",
        new DeserializationVisitorFactory());
  }
}
