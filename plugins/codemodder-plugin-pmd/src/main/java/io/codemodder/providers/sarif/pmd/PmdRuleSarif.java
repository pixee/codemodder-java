package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class PmdRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<String, List<Result>> resultsByFile;
  private final Path repositoryRoot;
  private Set<String> relativeLocations;

  PmdRuleSarif(
      final String ruleId,
      final SarifSchema210 sarif,
      final Map<String, List<Result>> resultsByFile,
      final Path repositoryRoot) {
    this.ruleId = Objects.requireNonNull(ruleId);
    this.sarif = Objects.requireNonNull(sarif);
    this.resultsByFile = Objects.requireNonNull(resultsByFile);
    this.repositoryRoot = repositoryRoot;
    this.relativeLocations = null;
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

  @Override
  public Set<String> getPaths() {
    if (this.relativeLocations == null) {
      relativeLocations =
          resultsByFile.keySet().stream()
              .map(s -> repositoryRoot.relativize(Path.of(s)).toString())
              .collect(Collectors.toSet());
    }
    return relativeLocations;
  }
}
