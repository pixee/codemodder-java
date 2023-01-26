package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class XMLDecoderTest {

  @Test
  void it_prevents_xml_decode() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/XMLDecoderVulnerability.java",
        new XMLDecoderVisitorFactory());
  }
}
