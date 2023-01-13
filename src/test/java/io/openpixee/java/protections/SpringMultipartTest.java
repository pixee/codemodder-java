package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class SpringMultipartTest {

  @Test
  void it_sanitizes_spring_multipart() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SpringMultipartVulnerability.java",
        new SpringMultipartVisitorFactory());
  }
}
