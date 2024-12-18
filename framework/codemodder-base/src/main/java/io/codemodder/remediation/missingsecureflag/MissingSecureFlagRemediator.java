package io.codemodder.remediation.missingsecureflag;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Remediator for missing secure flag in cookies. */
public final class MissingSecureFlagRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public MissingSecureFlagRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
                                .filter(mce -> "addCookie".equals(mce.getNameAsString()))
                                .filter(mce -> mce.getArguments().size() == 1)
                                .isPresent())
                    .build(),
                new FixAtJakartaAddCookieCallStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(
                                    n ->
                                        n instanceof ObjectCreationExpr
                                            ? (ObjectCreationExpr) n
                                            : null)
                                .filter(oce -> "Cookie".equals(oce.getTypeAsString()))
                                .filter(oce -> oce.getArguments().size() == 2)
                                .isPresent())
                    .build(),
                new FixAtJakartaCookieCreationStrategy())
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
