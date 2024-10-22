package io.codemodder.remediation.predictableseed;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Remediator for predictable seed weaknesses. */
public final class PredictableSeedRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public PredictableSeedRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.of(n)
                                .map(MethodOrConstructor::new)
                                .filter(mce -> mce.isMethodCallWithName("setSeed"))
                                .filter(mce -> mce.asNode().hasScope())
                                .filter(mce -> mce.getArguments().size() == 1)
                                // technically, we don't need this, just to prevent a silly tool
                                // from
                                // reporting on hardcoded data
                                .filter(mce -> !(mce.getArguments().get(0) instanceof LiteralExpr))
                                .isPresent())
                    .build(),
                new RemediationStrategy() {
                  @Override
                  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
                    MethodCallExpr setSeedCall = (MethodCallExpr) node;
                    MethodCallExpr safeExpression =
                        new MethodCallExpr(
                            new NameExpr(System.class.getSimpleName()), "currentTimeMillis");
                    NodeList<Expression> arguments = setSeedCall.getArguments();
                    arguments.set(0, safeExpression);
                    return SuccessOrReason.success();
                  }
                })
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
