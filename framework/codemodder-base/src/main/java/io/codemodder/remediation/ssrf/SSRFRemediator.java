package io.codemodder.remediation.ssrf;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.Either;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public final class SSRFRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public SSRFRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.<Either<MethodCallExpr, ObjectCreationExpr>>empty()
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(
                                                m ->
                                                    m instanceof ObjectCreationExpr
                                                        ? (ObjectCreationExpr) m
                                                        : null)
                                            .filter(m -> "URL".equals(m.getTypeAsString()))
                                            .filter(m -> m.getArguments().isNonEmpty())
                                            .map(Either::right))
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(
                                                m ->
                                                    m instanceof MethodCallExpr
                                                        ? (MethodCallExpr) m
                                                        : null)
                                            .filter(Node::hasScope)
                                            .filter(m -> "exchange".equals(m.getNameAsString()))
                                            .map(Either::left))
                                .isPresent())
                    .build(),
                new SSRFFixStrategy())
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
