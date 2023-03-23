package io.openpixee.java.protections;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class SSRFTest {

  @Test
  void it_protects_ssrf() throws IOException {
    String s =
        Path.of("src/test/java/com/acme/testcode/SSRFVulnerability.java").getFileName().toString();
    System.out.println(s);
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSRFVulnerability.java", new SSRFVisitorFactory());
  }
}
