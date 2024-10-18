package io.codemodder.remediation.jndiinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Remediates JNDI injection vulnerabilities. */
public final class JNDIInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  /** Remediator with the default strategy. */
  public JNDIInjectionRemediator() {
    this(new ReplaceLimitedLookupStrategy());
  }

  /**
   * A remediator with a chosen strategy
   *
   * @param strategy
   */
  public JNDIInjectionRemediator(final RemediationStrategy strategy) {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.of(n)
                                .map(MethodOrConstructor::new)
                                .filter(mce -> mce.isMethodCallWithName("lookup"))
                                .filter(mce -> mce.asNode().hasScope())
                                .filter(mce -> mce.getArguments().size() == 1)
                                .filter(mce -> mce.getArguments().get(0) instanceof NameExpr)
                                .isPresent())
                    .build(),
                strategy)
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
      Function<T, Optional<Integer>> findingStartColumnExtractor) {
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
}
