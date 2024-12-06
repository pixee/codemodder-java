package io.codemodder.remediation.javadeserialization;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/**
 * Fixes issues of related to object deserialization
 *
 * @param <T>
 */
public final class JavaDeserializationRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public JavaDeserializationRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                // matches declarations
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.empty()
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(
                                                m ->
                                                    m instanceof VariableDeclarationExpr vde
                                                        ? vde
                                                        : null)
                                            .filter(JavaDeserializationFixStrategy::match))
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(m -> m instanceof MethodCallExpr mce ? mce : null)
                                            .filter(JavaDeserializationFixStrategy::match))
                                .isPresent())
                    .build(),
                new JavaDeserializationFixStrategy())
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
