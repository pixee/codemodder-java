package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class XMLDecoderTest {

  @Test
  void it_prevents_xml_decode() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/XMLDecoderVulnerability.java",
        new XMLDecoderVisitorFactory());
  }
}
