package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openpixee.security.SystemCommand;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@inheritDoc} */
final class DefaultSemgrepRunner implements SemgrepRunner {

  /** {@inheritDoc} */
  @Override
  public SarifSchema210 runWithSingleRule(final Path rule, final Path repository)
      throws IOException {
    String ruleDirectoryPath = rule.toString();
    String repositoryPath = repository.toString();
    File sarifFile = File.createTempFile("semgrep", ".sarif");
    sarifFile.deleteOnExit();
    Process p =
        SystemCommand.runCommand(
            Runtime.getRuntime(),
            new String[] {
              "semgrep",
              "--sarif",
              "-o",
              sarifFile.getAbsolutePath(),
              "--config",
              ruleDirectoryPath,
              repositoryPath
            });
    try {
      int rc = p.waitFor();
      if (rc != 0) {
        throw new RuntimeException("error code seen from semgrep execution: " + rc);
      }
    } catch (InterruptedException e) {
      logger.error("problem waiting for semgrep process execution", e);
    }

    return new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class);
  }

  private static final Logger logger = LoggerFactory.getLogger(SemgrepRunner.class);
}
