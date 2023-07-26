package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PmdRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<Path, List<Result>> resultsCache;

  public PmdRuleSarif(final String ruleId, final SarifSchema210 sarif) {
    this.ruleId = Objects.requireNonNull(ruleId);
    this.sarif = Objects.requireNonNull(sarif);
    this.resultsCache = new HashMap<>();
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    return getResultsByPath(path).stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .toList();
  }

  @Override
  public List<Result> getResultsByPath(Path path) {
    if (resultsCache.containsKey(path)) {
      return resultsCache.get(path);
    }
    List<Result> results =
        sarif.getRuns().get(0).getResults().stream()
            .filter(result -> ruleId.endsWith("/" + result.getRuleId()))
            .filter(
                result -> {
                  String uri =
                      result
                          .getLocations()
                          .get(0)
                          .getPhysicalLocation()
                          .getArtifactLocation()
                          .getUri();
                  try {
                    return Files.isSameFile(path, Path.of(new URI(uri)));
                  } catch (IOException | URISyntaxException e) { // this should never happen
                    throw new IllegalStateException(e);
                  }
                })
            .toList();
    resultsCache.put(path, results);
    return results;
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
