package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

class POMTestHelper {
  static Document getEffectivePom(ProjectModel projectModel) throws IOException {
    File tmpInputFile = File.createTempFile("tmp-pom-orig", ".xml");
    File tmpOutputFile = File.createTempFile("tmp-pom", ".xml");

    // Write resultPomBytes to tmpInputFile
    try (FileOutputStream fos = new FileOutputStream(tmpInputFile)) {
      fos.write(projectModel.getPomFile().getResultPomBytes());
    }

    List<String> processArgs = new ArrayList<>();
    processArgs.add(Util.which("mvn").getAbsolutePath());
    processArgs.add("-B");
    processArgs.add("-N");
    processArgs.add("-f");
    processArgs.add(tmpInputFile.getAbsolutePath());

    if (SystemUtils.IS_OS_WINDOWS) {
      processArgs.add(0, "cmd.exe");
      processArgs.add(1, "/c");
    }

    if (!projectModel.getActiveProfiles().isEmpty()) {
      processArgs.add("-P");
      processArgs.add(String.join(",", projectModel.getActiveProfiles()));
    }

    processArgs.add("help:effective-pom");
    processArgs.add("-Doutput=" + tmpOutputFile.getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
    processBuilder.inheritIO();
    processBuilder.environment().putAll(System.getenv());

    Process process = processBuilder.start();

    try {
      int retCode = process.waitFor();

      if (retCode != 0) {
        throw new IllegalStateException("Unexpected return code from Maven: " + retCode);
      }

      return new SAXReader().read(new FileInputStream(tmpOutputFile));
    } catch (InterruptedException | DocumentException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Maven process was interrupted.", e);
    } finally {
      // Clean up temporary files
      tmpInputFile.delete();
      tmpOutputFile.delete();
    }
  }
}
