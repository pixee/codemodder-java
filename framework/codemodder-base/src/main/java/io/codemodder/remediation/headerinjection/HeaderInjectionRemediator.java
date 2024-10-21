package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.*;
import java.util.function.Function;

final class HeaderInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  private static final Set<String> setHeaderNames = Set.of("setHeader", "addHeader");

  public HeaderInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
                                .filter(Node::hasScope)
                                .filter(mce -> setHeaderNames.contains(mce.getNameAsString()))
                                .filter(mce -> mce.getArguments().size() == 2)
                                .filter(mce -> !(mce.getArgument(1) instanceof StringLiteralExpr))
                                .isPresent())
                    .build(),
                new HeaderInjectionFixStrategy())
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
