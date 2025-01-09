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

/** Remediator for XXE vulnerabilities anchored to the XMLStreamReader calls. */
public final class XMLStreamReaderIntermediateRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public XMLStreamReaderIntermediateRemediator() {
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
                new XMLStreamReaderIntermediateFixStrategy())
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
      final Function<T, Optional<Integer>> findingColumnExtractor) {
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
