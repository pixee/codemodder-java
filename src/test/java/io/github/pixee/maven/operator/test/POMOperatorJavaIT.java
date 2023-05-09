package io.github.pixee.maven.operator.test;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import io.github.pixee.maven.operator.Util;

import static org.junit.Assert.assertEquals;

public class POMOperatorJavaIT {
  @Test
  public void testJavaSample() throws Exception {
    String mvnAbsPath = Util.INSTANCE.which$pom_operator("mvn").getAbsolutePath();

    List<String> argList = Arrays.asList(
        mvnAbsPath,
        "-B",
        "-N",
        "-f",
        "java-sample/pom.xml",
        "verify"
    );

    if (SystemUtils.IS_OS_WINDOWS) {
      List<String> newArgList = Arrays.asList(Util.INSTANCE.which$pom_operator("cmd").getAbsolutePath(), "/c");

      newArgList.addAll(argList);

      argList = newArgList;
    }

    String[] args = argList.toArray(new String[0]);

    ProcessBuilder psBuilder = new ProcessBuilder(args).inheritIO();

    psBuilder.environment().putAll(System.getenv());

    Process process = psBuilder.start();

    int retCode = process.waitFor();

    assertEquals("Embedded execution must return zero", 0, retCode);
  }
}
