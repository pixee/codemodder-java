package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class UnsafeReadlineTest {

  @Test
  void it_prevents_unsafe_readline() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/UnsafeReadlineVulnerability.java",
        new UnsafeReadlineVisitorFactory());
  }
}
