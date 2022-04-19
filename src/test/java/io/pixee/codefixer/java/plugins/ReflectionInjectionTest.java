package io.pixee.codefixer.java.plugins;

import static io.pixee.codefixer.java.Results.buildSimpleResult;
import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ReflectionInjectionTest {

  @Test
  void it_fixes_reflection_injection() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/ReflectionInjectionVulnerability.java";
    assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new ReflectionInjectionVisitorFactory(
            new File("."),
            Set.of(
                buildSimpleResult(insecureFilePath, 11, "forName", "reflection-injection"),
                buildSimpleResult(insecureFilePath, 19, "forName", "reflection-injection"))));
  }
}
