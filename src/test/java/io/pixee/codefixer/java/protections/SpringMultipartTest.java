package io.pixee.codefixer.java.protections;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.pixee.codefixer.java.protections.WeavingNgTests.assertJavaWeaveWorkedAndWontReweave;

final class SpringMultipartTest {

  @Test
  void it_sanitizes_spring_multipart() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
            "src/test/java/com/acme/testcode/SpringMultipartVulnerability.java",
            new SpringMultipartVisitorFactoryNg());
  }
}
