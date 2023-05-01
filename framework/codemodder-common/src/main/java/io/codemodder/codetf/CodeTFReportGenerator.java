package io.codemodder.codetf;

import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
    return new Default();
  }

  class Default implements CodeTFReportGenerator {
    @Override
    public CodeTFReport createReport(
        final Path repositoryRoot,
        final String commandLine,
        final List<Path> sarifs,
        final List<CodeTFResult> results,
        final long elapsed) {

      List<CodeTFSarifInput> sarifInputs =
          sarifs.stream()
              .map(
                  sarif -> {
                    try {
                      return new CodeTFSarifInput(getSha1(sarif), Files.readString(sarif));
                    } catch (IOException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .collect(Collectors.toList());

      CodeTFRun run =
          new CodeTFRun(
              "io.codemodder",
              "codemodder",
              "1.0.0",
              commandLine,
              elapsed,
              repositoryRoot.toString(),
              sarifInputs);

      return new CodeTFReport(run, results);
    }

    /** Get a SHA-1 of the given file. */
    private String getSha1(final Path file) {
      try {
        return sha1Hex(Files.newInputStream(file));
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to sha1 sarif file", e);
      }
    }
  }
}
