package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class XXETest {

  @Test
  void it_prevents_xxe() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/XXEVulnerability.java", new XXEVisitorFactory());
  }
}
