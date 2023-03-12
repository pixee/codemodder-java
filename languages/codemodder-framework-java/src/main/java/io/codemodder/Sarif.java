package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper around {@link com.contrastsecurity.sarif.SarifSchema210} that also provides convenience
 * methods that make writing codemods easier.
 */
public final class Sarif {

  private Sarif() {}

  /**
   * Get all the {@link Result} that are from the given {@link CompilationUnit}.
   *
   * @param sarif the SARIF
   * @param filePath the file that's being scanned
   * @return a {@link List} containing the SARIF results for this file
   */
  public static List<Result> getResultsForCompilationUnit(
      final SarifSchema210 sarif, final Path filePath) {
    List<Result> results = sarif.getRuns().get(0).getResults();
    return results.stream()
        .filter(
            result -> {
              String uri =
                  result.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri();
              try {
                return Files.isSameFile(filePath, Path.of(uri));
              } catch (IOException e) { // this should never happen
                return false;
              }
            })
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Get all of the {@link Region} entries for the given {@link Result} list.
   *
   * @param results the results to map to source code locations
   * @return a list of source code locations
   */
  public static List<Region> findRegions(final List<Result> results) {
    return results.stream()
        .map(result -> result.getLocations().get(0).getPhysicalLocation().getRegion())
        .collect(Collectors.toUnmodifiableList());
  }
}
