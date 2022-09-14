package io.openpixee.maven.operator.test;

import org.junit.Test;

import static org.junit.Assert.*;

public class POMOperatorJavaIT {
  @Test
  public void testJavaSample() throws Exception {
    ProcessBuilder psBuilder = new ProcessBuilder(
        "mvn",
        "-B",
        "-N",
        "-f",
        "java-sample/pom.xml",
        "verify"
        ).inheritIO();

    psBuilder.environment().putAll(System.getenv());

    Process process = psBuilder.start();

    int retCode = process.waitFor();

    assertEquals("Embedded execution must return zero", 0, retCode);
  }
}
