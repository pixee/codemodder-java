package io.codemodder.codetf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;

final class DefaultCodeTFReportGenerator implements CodeTFReportGenerator {

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
      return DigestUtils.sha1Hex(Files.newInputStream(file));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to sha1 sarif file", e);
    }
  }
}
