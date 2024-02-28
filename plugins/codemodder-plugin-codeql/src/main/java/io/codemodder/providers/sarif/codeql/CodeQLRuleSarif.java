package io.codemodder.providers.sarif.codeql;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
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

/** {@inheritDoc} An implementation of the {@link RuleSarif} for SARIFs produced by CodeQL. */
public final class CodeQLRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<Path, List<Result>> resultsCache;
  private final Path repositoryRoot;

  public CodeQLRuleSarif(
      final String ruleId, final SarifSchema210 sarif, final Path repositoryRoot) {
    this.sarif = Objects.requireNonNull(sarif);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.repositoryRoot = repositoryRoot;
    this.resultsCache = new HashMap<>();
  }

  private String extractRuleId(final Result result, final Run run) {
    if (result.getRuleId() == null) {
      var toolIndex = result.getRule().getToolComponent().getIndex();
      var ruleIndex = result.getRule().getIndex();
      var maybeRule =
          run.getTool().getExtensions().stream()
              .skip(toolIndex)
              .findFirst()
              .flatMap(tool -> tool.getRules().stream().skip(ruleIndex).findFirst())
              .map(rd -> rd.getId());
      if (maybeRule.isPresent()) {
        return maybeRule.get();
      } else {
        return null;
      }
    }
    return result.getRuleId();
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    return getResultsByLocationPath(path).stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Result> getResultsByLocationPath(final Path path) {
    if (resultsCache.containsKey(path)) {
      return resultsCache.get(path);
    }
    List<Result> results =
        sarif.getRuns().stream()
            .flatMap(
                run ->
                    run.getResults().stream()
                        .filter(result -> ruleId.equals(extractRuleId(result, run))))
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
                    Path uriPath = Path.of(uri);
                    if (Files.exists(repositoryRoot.resolve(uriPath))) {
                      return Files.isSameFile(path, repositoryRoot.resolve(uriPath));
                    } else {
                      return false;
                    }
                  } catch (IOException e) { // this should never happen
                    logger.error("Problem inspecting SARIF to find code results", e);
                    return false;
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

  static final String toolName = "CodeQL";

  private static final Logger logger = LoggerFactory.getLogger(CodeQLRuleSarif.class);
}
