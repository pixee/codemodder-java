package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** {@inheritDoc} */
public final class SemgrepSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;

  SemgrepSarif(final String ruleId, final SarifSchema210 sarif) {
    this.sarif = Objects.requireNonNull(sarif);
    this.ruleId = Objects.requireNonNull(ruleId);
  }

  /** {@inheritDoc} */
  @Override
  public SarifSchema210 rawDocument() {
    return sarif;
  }

  /** {@inheritDoc} */
  @Override
  public String getRule() {
    return ruleId;
  }

  /** {@inheritDoc} */
  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path, final String ruleId) {
    List<Result> resultsFilteredByRule =
        sarif.getRuns().get(0).getResults().stream()
            .filter(result -> result.getRuleId().endsWith("." + ruleId))
            .collect(Collectors.toUnmodifiableList());
    List<Result> resultsFilteredByRuleAndPath =
        resultsFilteredByRule.stream()
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
                    return Files.isSameFile(path, Path.of(uri));
                  } catch (IOException e) { // this should never happen
                    return false;
                  }
                })
            .collect(Collectors.toUnmodifiableList());
    return resultsFilteredByRuleAndPath.stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .collect(Collectors.toUnmodifiableList());
  }
}
