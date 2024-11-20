package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class XXEIntermediateXMLStreamReaderRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public XXEIntermediateXMLStreamReaderRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof Expression e ? e : null)
                                .flatMap(ASTs::isArgumentOfMethodCall)
                                .filter(Node::hasScope)
                                .isPresent())
                    .build(),
                new XXEIntermediateXMLStreamReaderFixStrategy())
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
