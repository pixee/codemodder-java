package io.codemodder.remediation.javadeserialization;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
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
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.empty()
                                .or(
                                    () ->
                                        Optional.of(node)
                                            .map(
                                                n ->
                                                    n instanceof MethodCallExpr
                                                        ? (MethodCallExpr) n
                                                        : null)
                                            .filter(Node::hasScope)
                                            .filter(
                                                mce -> mce.getNameAsString().equals("readObject"))
                                            .filter(mce -> mce.getArguments().isEmpty()))
                                .or(
                                    () ->
                                        Optional.of(node)
                                            .map(
                                                n ->
                                                    n instanceof ObjectCreationExpr
                                                        ? (ObjectCreationExpr) n
                                                        : null)
                                            .filter(
                                                oce ->
                                                    "ObjectInputStream"
                                                        .equals(oce.getTypeAsString())))
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
