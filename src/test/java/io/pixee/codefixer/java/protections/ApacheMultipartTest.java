package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class ApacheMultipartTest {

  @Test
  void it_sanitizes_apache_multipart() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/ApacheMultipartVulnerability.java",
        new ApacheMultipartVisitorFactory());
  }
}
