package io.codemodder.remediation.weakrandom;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public final class WeakRandomRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public WeakRandomRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.of(n)
                                .map(
                                    m ->
                                        m instanceof ObjectCreationExpr
                                            ? (ObjectCreationExpr) m
                                            : null)
                                .filter(oc -> oc.getType().asString().equals("Random"))
                                .isPresent())
                    .build(),
                new WeakRandomFixStrategy())
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
