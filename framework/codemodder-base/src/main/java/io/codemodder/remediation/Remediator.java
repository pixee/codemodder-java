package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Remediates findings for a given path.
 *
 * @param <T>
 */
public interface Remediator<T> {

  /** Performs the work of performing all remediation, and returning reporting metadata. */
  CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingStartLineExtractor,
      final Function<T, Optional<Integer>> findingEndLineExtractor,
      final Function<T, Optional<Integer>> findingStartColumnExtractor);
}
