package io.openpixee.java.protections;

import static io.openpixee.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class PredictableSeedTest {

  @Test
  void it_replaces_insecure_seed() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/PredictableSeedVulnerability.java",
        new PredictableSeedVisitorFactory());
  }
}
