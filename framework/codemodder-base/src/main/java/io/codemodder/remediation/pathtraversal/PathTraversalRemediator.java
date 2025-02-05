package io.codemodder.remediation.pathtraversal;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Remediate path traversal vulns. */
public final class PathTraversalRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public PathTraversalRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
                                .filter(PathTraversalRemediator::isSpringMultipartFilenameCall)
                                .isPresent())
                    .build(),
                new SpringMultipartFixStrategy())
            .build();
  }

  @Override
  public CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingStartLineExtractor,
      final Function<T, Optional<Integer>> findingEndLineExtractor,
      final Function<T, Optional<Integer>> findingStartColumnExtractor) {
    return searchStrategyRemediator.remediateAll(
        cu,
        path,
        detectorRule,
        findingsForPath,
        findingIdExtractor,
        findingStartLineExtractor,
        findingEndLineExtractor,
        findingStartColumnExtractor);
  }

  private static boolean isSpringMultipartFilenameCall(final MethodCallExpr methodCallExpr) {
    return methodCallExpr.hasScope()
        && "getOriginalFilename".equals(methodCallExpr.getNameAsString());
  }
}
