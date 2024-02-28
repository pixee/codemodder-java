package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PmdRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<String, List<Result>> resultsByFile;

  PmdRuleSarif(
      final String ruleId,
      final SarifSchema210 sarif,
      final Map<String, List<Result>> resultsByFile) {
    this.ruleId = Objects.requireNonNull(ruleId);
    this.sarif = Objects.requireNonNull(sarif);
    this.resultsByFile = Objects.requireNonNull(resultsByFile);
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    return getResultsByLocationPath(path).stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .toList();
  }

  @Override
  public List<Result> getResultsByLocationPath(final Path path) {
    String absolutePath = path.toAbsolutePath().toString();
    return resultsByFile.getOrDefault(absolutePath, List.of());
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

  static final String toolName = "pmd";
}
