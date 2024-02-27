package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** A {@link RuleSarif} for AppScan results. */
final class AppScanRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<Path, List<Result>> resultsCache;
  private final Path repositoryRoot;

  public AppScanRuleSarif(
      final String ruleId, final SarifSchema210 sarif, final Path repositoryRoot) {
    this.sarif = Objects.requireNonNull(sarif);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.repositoryRoot = repositoryRoot;
    this.resultsCache = new HashMap<>();
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    return List.of();
  }

  @Override
  public List<Result> getResultsByPath(final Path path) {
    return List.of();
  }

  @Override
  public SarifSchema210 rawDocument() {
    return sarif;
  }

  @Override
  public String getRule() {
    return ruleId;
  }

  @Override
  public String getDriver() {
    return toolName;
  }

  static final String toolName = "HCL AppScan Static Analyzer";
}
