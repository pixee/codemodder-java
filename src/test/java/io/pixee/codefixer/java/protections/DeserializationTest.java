package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingNgTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class DeserializationTest {

  @Test
  void it_prevents_deserialization() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/DeserializationVulnerability.java",
        new DeserializationVisitorFactoryNg());
  }
}
