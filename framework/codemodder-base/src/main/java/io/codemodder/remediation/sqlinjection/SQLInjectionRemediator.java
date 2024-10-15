package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.*;
import java.util.function.Function;

/**
 * A Remediator for SQL injection issues. This class provides the logic to visit a CompilationUnit
 * and process findings for potential SQL injections.
 */
public final class SQLInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public SQLInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n -> Optional.of(n).filter(SQLInjectionFixComposer::match).isPresent())
                    .build(),
                new SQLInjectionFixComposer())
            .build();
  }

  @Override
  public CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      Collection<T> findingsForPath,
      Function<T, String> findingIdExtractor,
      Function<T, Integer> findingStartLineExtractor,
      Function<T, Optional<Integer>> findingEndLineExtractor,
      Function<T, Optional<Integer>> findingColumnExtractor) {
    return searchStrategyRemediator.remediateAll(
        cu,
        path,
        detectorRule,
        findingsForPath,
        findingIdExtractor,
        findingStartLineExtractor,
        findingEndLineExtractor,
        findingColumnExtractor);
  }
}
