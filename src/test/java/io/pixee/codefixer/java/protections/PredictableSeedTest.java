package io.pixee.codefixer.java.protections;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

final class PredictableSeedTest {

  @Test
  void it_replaces_insecure_seed() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/PredictableSeedVulnerability.java", new PredictableSeedVisitorFactory());
  }
}
