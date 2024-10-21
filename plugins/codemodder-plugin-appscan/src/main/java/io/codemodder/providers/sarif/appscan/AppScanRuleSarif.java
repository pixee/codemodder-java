package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.*;
import io.codemodder.CodeDirectory;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;

/** A {@link RuleSarif} for AppScan results. */
final class AppScanRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String messageText;
  private final Map<Path, List<Result>> resultsCache;
  private final List<String> locations;

  /** A map of a AppScan SARIF "location" URIs mapped to their respective file paths. */
  private final Map<Path, Set<Integer>> artifactLocationIndices;

  /**
   * Creates an {@link AppScanRuleSarif} that has already done the work of mapping AppScan SARIF
   * locations, which are strange combinations of class name and file path, into predictable paths.
   */
  AppScanRuleSarif(
      final String messageText, final SarifSchema210 sarif, final CodeDirectory codeDirectory) {
    this.sarif = Objects.requireNonNull(sarif);
    this.messageText = Objects.requireNonNull(messageText);
    this.resultsCache = new HashMap<>();
    this.locations =
        sarif.getRuns().get(0).getArtifacts().stream()
            .map(Artifact::getLocation)
            .map(ArtifactLocation::getUri)
            .map(u -> u.substring(8)) // trim the file:/// of all results
            .toList();
    Map<Path, Set<Integer>> artifactLocationIndicesMap = new HashMap<>();

    for (int i = 0; i < locations.size(); i++) {
      final Integer index = i;
      String path = locations.get(i);
      path = path.replace('\\', '/');
      // we have a real but partial path, now we have to find it in the repository
      Optional<Path> existingRealPath;
      try {
        existingRealPath = codeDirectory.findFilesWithTrailingPath(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      // add to the map if we found a matching file
      existingRealPath.ifPresent(
          p -> artifactLocationIndicesMap.computeIfAbsent(p, k -> new HashSet<>()).add(index));
    }
    this.artifactLocationIndices = Map.copyOf(artifactLocationIndicesMap);
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
}
