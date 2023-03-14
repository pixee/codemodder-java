package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Responsible for running semgrep */
interface SemgrepRunner {

  /**
   * Execute semgrep.
   *
   * @param yamls the directory/file(s) where the rule(s) are stored
   * @param codeDir the directory containing the code to be run on
   * @return the resulting SARIF
   */
  SarifSchema210 run(List<Path> yamls, Path codeDir) throws IOException;
}
