package io.pixee.codefixer.java.protections;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class VerbTamperingTest {

  @Test
  void it_prevents_verb_tampering_vuln() throws IOException {
    WeavingTests.assertFileWeaveWorkedAndReweave(
        "src/test/resources/web.xml", new VerbTamperingVisitor());
  }
}
