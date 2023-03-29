package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class ZipSlipTest {

  @Test
  void it_hardens_zipinputstreams() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/ZipSlipVulnerability.java",
        new ZipFileOverwriteVisitoryFactory());
  }
}
