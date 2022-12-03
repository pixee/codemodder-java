package io.openpixee.java.protections;

import static io.openpixee.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;

import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * This disabled test illustrates our amibitions of solving SQL Injection using more advanced
 * rewriting capabilities.
 */
final class SQLInjectionTest {

  @Disabled
  @Test
  void it_rewrites_parameterized() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SQLSimpleSelectVulnerability.java",
        new WeakPRNGVisitorFactory());
  }
}
