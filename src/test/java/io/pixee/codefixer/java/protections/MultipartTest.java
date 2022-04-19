package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class MultipartTest {
  @Test
  void it_sanitizes_multipart() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/MultipartVulnerability.java",
        new MultipartVisitorFactory());
  }
}
