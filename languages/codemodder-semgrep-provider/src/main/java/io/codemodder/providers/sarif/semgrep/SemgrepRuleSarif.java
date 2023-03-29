package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * <p>Semgrep's SARIF has a relatively simple model but the "ruleId" in the document is weird. It
 * places the whole path to the rule in the field. This means our filtering logic is a little weird
 * and unexpected, but it works.
 */
public final class SemgrepRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<Path, List<Result>> resultsCache;

  SemgrepRuleSarif(final String ruleId, final SarifSchema210 sarif) {
    this.sarif = Objects.requireNonNull(sarif);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.resultsCache = new HashMap<>();
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
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    return getResultsByPath(path).stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Result> getResultsByPath(final Path path) {
    if (resultsCache.containsKey(path)) {
      return resultsCache.get(path);
    }
    List<Result> results =
        sarif.getRuns().get(0).getResults().stream()
            .filter(result -> result.getRuleId().endsWith("." + ruleId))
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
                    logger.error("Problem inspecting SARIF to find code results", e);
                    return false;
                  }
                })
            .collect(Collectors.toUnmodifiableList());
    resultsCache.put(path, results);
    return results;
  }

  private static final Logger logger = LoggerFactory.getLogger(SemgrepRuleSarif.class);
}
