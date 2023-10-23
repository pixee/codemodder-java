package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Responsible for running semgrep */
public interface SemgrepRunner {

  /**
   * Execute semgrep.
   *
   * @param yamls the file(s) where the rule(s) are stored
   * @param codeDir the directory containing the code to be run on
   * @return the resulting SARIF
   */
  SarifSchema210 run(
      List<Path> yamls, Path codeDir, List<String> includePatterns, List<String> excludePatterns)
      throws IOException;

  static SemgrepRunner createDefault() {
    return new DefaultSemgrepRunner();
  }
}
