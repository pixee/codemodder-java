package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.*;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link RuleSarif} for AppScan results. */
final class AppScanRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String messageText;
  private final Map<Path, List<Result>> resultsCache;
  private final AppScanSarifLocationData sarifLocationData;

  /**
   * Creates an {@link AppScanRuleSarif} that has already done the work of mapping AppScan SARIF
   * locations, which are strange combinations of class name and file path, into predictable paths.
   */
  AppScanRuleSarif(
      final String messageText,
      final SarifSchema210 sarif,
      final AppScanSarifLocationData sarifLocationData) {
    this.sarif = Objects.requireNonNull(sarif);
    this.messageText = Objects.requireNonNull(messageText);
    this.resultsCache = new HashMap<>();
    this.sarifLocationData = Objects.requireNonNull(sarifLocationData);
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    List<Result> resultsByLocationPath = getResultsByLocationPath(path);
    return resultsByLocationPath.stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .toList();
  }

  /**
   * This call receives an actual source file path, whereas the AppScan results store a reference to
   * a fully qualified class name plus ".java", e.g.:
   *
   * <pre>file:///org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java</pre>
   */
  @Override
  public List<Result> getResultsByLocationPath(final Path path) {

    Map<Path, Set<Integer>> artifactLocationIndices =
        sarifLocationData.getArtifactLocationIndices();
    return resultsCache.computeIfAbsent(
        path,
        p ->
            sarif.getRuns().stream()
                .flatMap(run -> run.getResults().stream())
                .filter(result -> result.getMessage().getText().equals(messageText))
                .filter(
                    result ->
                        artifactLocationIndices.get(path) != null
                            && artifactLocationIndices
                                .getOrDefault(path, Set.of())
                                .contains(
                                    result
                                        .getLocations()
                                        .get(0)
                                        .getPhysicalLocation()
                                        .getArtifactLocation()
                                        .getIndex()))
                .toList());
  }

  @Override
  public String getDriver() {
    return toolName;
  }

  /**
   * This returns the raw SARIF. This SARIF, for Java, contains binary analysis results. These
   * results may need a lot of massaging to act on.
   */
  @Override
  public SarifSchema210 rawDocument() {
    return sarif;
  }

  /**
   * This returns the "message[text]" field from the SARIF results. This is a human-readable value
   * like "SQL Injection". We would ordinarily use this as the rule ID but this value is different
   * each time we retrieve the SARIF for a given scan
   */
  @Override
  public String getRule() {
    return messageText;
  }

  static final String toolName = "HCL AppScan Static Analyzer";

  private static final Logger log = LoggerFactory.getLogger(AppScanRuleSarif.class);
}
