package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.*;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** A {@link RuleSarif} for AppScan results. */
final class AppScanRuleSarif implements RuleSarif {

  private final SarifSchema210 sarif;
  private final String ruleId;
  private final Map<Path, List<Result>> resultsCache;
  private final Path repositoryRoot;
  private final List<String> locations;

  /** A map of a HCL SARIF "location" URIs mapped to their respective file paths. */
  private final Map<Path, Set<Integer>> artifactLocationIndices;

  /**
   * Creates an {@link AppScanRuleSarif} that has already done the work of mapping HCL SARIF
   * locations, which are strange combinations of class name and file path, into predictable paths.
   */
  public AppScanRuleSarif(
      final String ruleId, final SarifSchema210 sarif, final Path repositoryRoot) {
    this.sarif = Objects.requireNonNull(sarif);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.repositoryRoot = repositoryRoot;
    this.resultsCache = new HashMap<>();
    this.locations =
        sarif.getRuns().get(0).getArtifacts().stream()
            .map(Artifact::getLocation)
            .map(ArtifactLocation::getUri)
            .map(u -> u.substring(8))
            .toList();
    Map<Path, Set<Integer>> artifactLocationIndices = new HashMap<>();

    for (int i = 0; i < locations.size(); i++) {
      final Integer index = i;
      String path = locations.get(i);
      path = path.replace('\\', '/');
      // we have a real but partial path, now we have to find it in the repository
      Optional<Path> existingRealPath;
      try {
        existingRealPath = findFileWithTrailingPath(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      // add to the map if we found a matching file
      existingRealPath.ifPresent(
          p ->
              artifactLocationIndices
                  .computeIfAbsent(p, k -> new HashSet<>())
                  .add(index));
    }
    this.artifactLocationIndices = Map.copyOf(artifactLocationIndices);
  }

  private Optional<Path> findFileWithTrailingPath(final String path) throws IOException {
    // find the files with the trailing path
    AtomicReference<Path> found = new AtomicReference<>();
    Files.walkFileTree(
        repositoryRoot,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            if (file.toString().endsWith(path)) {
              found.set(file);
              return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return Optional.ofNullable(found.get());
  }

  @Override
  public List<Region> getRegionsFromResultsByRule(final Path path) {
    List<Result> resultsByLocationPath = getResultsByLocationPath(path);
    return resultsByLocationPath.stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .toList();
  }

  /**
   * This call receives an actual source file path, whereas the HCL results store a reference to a
   * fully qualified class name plus ".java", e.g.:
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
                .filter(result -> result.getRuleId().equals(ruleId))
                .filter(
                    result ->
                        artifactLocationIndices.get(path) != null
                            && artifactLocationIndices
                                .get(path)
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
