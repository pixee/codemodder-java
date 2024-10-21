package io.codemodder.remediation.reflectioninjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public final class ReflectionInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public ReflectionInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
                                .filter(ReflectionInjectionRemediator::isClassForNameCall)
                                .isPresent())
                    .build(),
                new ReflectionInjectionFixStrategy())
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

  /**
   * Check if the method call expression is a call to {@code Class.forName(String)}.
   *
   * <p>This is important, because this codemod uses fuzzy region matching, to account for how Sonar
   * reports the region for this finding. Sonar reports the region to be only the "forName" part of
   * the method expression.
   *
   * @param methodCallExpr the node to check
   * @return true if the method call expression is a call to {@code Class.forName(String)}
   */
  private static boolean isClassForNameCall(final MethodCallExpr methodCallExpr) {
    var maybeCU = methodCallExpr.findCompilationUnit();
    if (maybeCU.isEmpty()) {
      return false;
    }
    var cu = maybeCU.get();
    final boolean scopeMatches =
        methodCallExpr
            .getScope()
            .map(
                expression -> {
                  if (expression.isNameExpr()) {
                    final var nameExpr = expression.asNameExpr();
                    return nameExpr.getNameAsString().equals("Class");
                  }
                  return false;
                })
            .orElse(
                cu.getImports().stream()
                    .anyMatch(
                        importDeclaration ->
                            importDeclaration.isStatic()
                                && importDeclaration
                                    .getNameAsString()
                                    .equals("java.lang.Class.forName")));
    final var methodNameMatches = methodCallExpr.getNameAsString().equals("forName");
    return scopeMatches && methodNameMatches;
  }
}
