package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.nio.file.Path;

/** Responsible for running semgrep */
interface SemgrepRunner {

  /**
   * Execute semgrep.
   *
   * @param rule the directory/file where the rule(s) are stored
   * @param repository the directory containing the code to be run on
   * @return the resulting SARIF
   */
  SarifSchema210 runWithSingleRule(Path rule, Path repository) throws IOException;
}
