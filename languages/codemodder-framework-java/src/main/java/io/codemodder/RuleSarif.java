package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;

/** Defines a model for interacting with SARIF. */
public interface RuleSarif {

  /**
   * Get all the regions for the SARIF with the matching rule ID
   *
   * @param path the file being scanned
   * @return the source code regions where the given rule was found in the given file
   */
  List<Region> getRegionsFromResultsByRule(Path path);

  /** Return the entire SARIF as a model in case more comprehensive inspection is needed. */
  SarifSchema210 rawDocument();

  /** Returns the string ID for the rule. */
  String getRule();
}
