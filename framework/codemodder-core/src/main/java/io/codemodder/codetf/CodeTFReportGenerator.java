package io.codemodder.codetf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * This type is responsible for generating a {@link CodeTFReport} based on the domain objects
 * representing our inputs and outputs.
 */
public interface CodeTFReportGenerator {

  /**
   * Creates the report.
   *
   * @param repositoryRoot the root directory of the repository
   * @param commandLine the command line that was used to run the tool
   * @param sarifs the sarif files that were used as input
   * @param results the results of the scan/fix
   * @param millisElapsed how long the scan/fix took
   * @return the {@link CodeTFReport} representing all the stuff above
   */
  CodeTFReport createReport(
      Path repositoryRoot,
      String commandLine,
      List<Path> sarifs,
      List<CodeTFResult> results,
      long millisElapsed)
      throws IOException;

  static CodeTFReportGenerator createDefault() {
    return new DefaultCodeTFReportGenerator();
  }
}
