package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.*;
import java.util.function.Function;

/**
 * Fixes header injection pointed by issues.
 *
 * @param <T>
 */
public final class HeaderInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public HeaderInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(HeaderInjectionFixMethodCallStrategy::matchMethodCall)
                    .build(),
                new HeaderInjectionFixMethodCallStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(HeaderInjectionFixMethodArgumentStrategy::matchMethodArgument)
                    .build(),
                new HeaderInjectionFixMethodArgumentStrategy())
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
