package io.openpixee.java.protections;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

final class VerbTamperingTest {

  @Disabled("should be restored if we change design to go bcak to visitor based approach")
  @Test
  void it_prevents_verb_tampering_vuln() {
    WeavingTests.assertFileWeaveWorkedAndReweave(
        "src/test/resources/web.xml", new VerbTamperingVisitor());
  }
}
