package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;

/** Defines a model for interacting with SARIF. */
public interface RuleSarif {

  /** A {@link RuleSarif} with no results. */
  RuleSarif EMPTY = new EmptyRuleSarif();

  /**
   * Get all the regions for the SARIF with the matching rule ID
   *
   * @param path the file being scanned
   * @return the source code regions where the given rule was found in the given file
   */
  List<Region> getRegionsFromResultsByRule(Path path);

  /**
   * Get all the SARIF results with the matching path for the first location field
   *
   * @param path the file being scanned
   * @return the results associated with the given file
   */
  List<Result> getResultsByLocationPath(Path path);

  /** Return the entire SARIF as a model in case more comprehensive inspection is needed. */
  SarifSchema210 rawDocument();

  /** Returns the string ID for the rule. */
  String getRule();

  /** Returns the tool driver that produced this SARIF. */
  String getDriver();

  /** An empty implementation of {@link RuleSarif} for binding codemods with no SARIF results. */
  final class EmptyRuleSarif implements RuleSarif {

    @Override
    public List<Region> getRegionsFromResultsByRule(final Path path) {
      return List.of();
    }

    @Override
    public List<Result> getResultsByLocationPath(final Path path) {
      return List.of();
    }

    @Override
    public String getRule() {
      return "";
    }

    @Override
    public String getDriver() {
      return "";
    }

    @Override
    public SarifSchema210 rawDocument() {
      return new SarifSchema210();
    }
  }
}
