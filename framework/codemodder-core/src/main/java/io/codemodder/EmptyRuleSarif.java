package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import java.nio.file.Path;
import java.util.List;

/** An empty implementation of {@link RuleSarif} for binding codemods with no SARIF results. */
public class EmptyRuleSarif implements RuleSarif {

  @Override
  public List<Region> getRegionsFromResultsByRule(Path path) {
    return List.of();
  }

  @Override
  public List<Result> getResultsByPath(Path path) {
    return List.of();
  }

  @Override
  public SarifSchema210 rawDocument() {
    return new SarifSchema210();
  }

  @Override
  public String getRule() {
    return "";
  }

  @Override
  public String getDriver() {
    return "";
  }
}
