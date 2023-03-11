package io.codemodder;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import java.util.List;

/**
 * A wrapper around {@link com.contrastsecurity.sarif.SarifSchema210} that also provides convenience
 * methods that make writing codemods easier.
 */
public final class Sarif {

  private Sarif() {}

  /**
   * Get all the {@link Result} that are from the given {@link CompilationUnit}.
   *
   * @param cu the source file
   * @return a {@link List} containing the SARIF results for this file
   */
  public static List<Result> getResultsForCompilationUnit(
      final SarifSchema210 sarif, final CompilationUnit cu) {
    throw new UnsupportedOperationException();
  }

  /**
   * Get all of the {@link Region} entries for the given {@link Result} list.
   *
   * @param results the results to map to source code locations
   * @return a list of source code locations
   */
  public static List<Region> findRegions(final List<Result> results) {
    throw new UnsupportedOperationException();
  }
}
