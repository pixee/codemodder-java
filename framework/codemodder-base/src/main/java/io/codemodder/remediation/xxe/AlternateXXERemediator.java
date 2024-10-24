package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.WithoutScopePositionMatcher;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class AlternateXXERemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public AlternateXXERemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(DocumentBuilderFactoryAndSAXParserAtCreationFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new DocumentBuilderFactoryAndSAXParserAtCreationFixStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(DocumentBuilderFactoryAtNewDBFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new DocumentBuilderFactoryAtNewDBFixStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(SAXParserAtNewSPFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new SAXParserAtNewSPFixStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(DocumentBuilderFactoryAtParseFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new DocumentBuilderFactoryAtParseFixStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(TransformerFactoryAtCreationFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new TransformerFactoryAtCreationFixStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(XMLReaderAtParseFixStrategy::match)
                    .withNodePositionMatcher(new WithoutScopePositionMatcher())
                    .build(),
                new XMLReaderAtParseFixStrategy())
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
