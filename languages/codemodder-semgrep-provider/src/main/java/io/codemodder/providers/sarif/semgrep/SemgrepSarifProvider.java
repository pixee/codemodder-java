package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.nio.file.Path;

/** A provider that invokes semgrep (assuming Semgrep is on the $PATH) */
public interface SemgrepSarifProvider {

  /**
   * Invokes Semgrep with given rule and return the SARIF
   *
   * @param rulePath the classpath location of a semgrep YAML rule
   */
  SarifSchema210 getSarif(Path repositoryPath, String rulePath) throws IOException;
}
