package io.openpixee.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class WeakPRNGTest {

  @Test
  void it_replaces_insecure_random() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RandomVulnerability.java");
  }
}
