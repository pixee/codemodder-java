package io.codemodder.remediation.loginjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Remediator for Log Injection vulnerabilities. */
public final class LogInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public LogInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        n ->
                            Optional.of(n)
                                .map(MethodOrConstructor::new)
                                .filter(mce -> mce.isMethodCallWithNameIn(loggerNames))
                                .filter(mce -> mce.asNode().hasScope())
                                .filter(mce -> !mce.getArguments().isEmpty())
                                .isPresent())
                    .build(),
                new LogStatementFixer())
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

  private static final Set<String> loggerNames =
      Set.of("log", "warn", "error", "info", "debug", "trace", "fatal");
}
