package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class XXERemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public XXERemediator() {
    this(NodePositionMatcher.DEFAULT);
  }

  public XXERemediator(final NodePositionMatcher matcher) {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withMatchAndFixStrategyAndNodeMatcher(
                new DocumentBuilderFactoryAndSAXParserAtCreationFixStrategy(), matcher)
            .withMatchAndFixStrategyAndNodeMatcher(
                new DocumentBuilderFactoryAtNewDBFixStrategy(), matcher)
            .withMatchAndFixStrategyAndNodeMatcher(new SAXParserAtNewSPFixStrategy(), matcher)
            .withMatchAndFixStrategyAndNodeMatcher(
                new DocumentBuilderFactoryAtParseFixStrategy(), matcher)
            .withMatchAndFixStrategyAndNodeMatcher(
                new TransformerFactoryAtCreationFixStrategy(), matcher)
            .withMatchAndFixStrategyAndNodeMatcher(new XMLReaderAtParseFixStrategy(), matcher)
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
