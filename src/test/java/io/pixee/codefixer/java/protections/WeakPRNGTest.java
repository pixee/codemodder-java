package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class WeakPRNGTest {

  @Test
  void it_replaces_insecure_random() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RandomVulnerability.java", new WeakPRNGVisitorFactory());
  }
}
