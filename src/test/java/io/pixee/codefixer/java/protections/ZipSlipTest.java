package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class ZipSlipTest {

  @Test
  void it_hardens_zipinputstreams() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/ZipSlipVulnerability.java",
        new ZipFileOverwriteVisitoryFactory());
  }
}
