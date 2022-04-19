package io.pixee.codefixer.java.plugins.contrast;

import static io.pixee.codefixer.java.Results.buildSimpleResult;
import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class JavaXssTest {

  @Test
  void it_prevents_xss() throws IOException {
    String insecureFilePath = "src/test/java/com/acme/testcode/XSSVulnerability.java";
    assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new JavaXssVisitorFactory(
            new File("."),
            Set.of(
                buildSimpleResult(insecureFilePath, 10, "print", "reflected-xxs"),
                buildSimpleResult(insecureFilePath, 11, "println", "reflected-xxs"),
                buildSimpleResult(insecureFilePath, 12, "printf", "reflected-xxs"),
                buildSimpleResult(insecureFilePath, 13, "write", "reflected-xxs")),
                "reflected-xss"));
  }
}
