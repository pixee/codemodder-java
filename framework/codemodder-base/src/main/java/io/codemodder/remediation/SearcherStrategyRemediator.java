package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.javatuples.Pair;

/**
 * Remediates issues with pairs of searchers and strategies. Searchers will associate an issue with
 * a node from the AST, while a strategy will try to fix an issue.
 *
 * @param <T>
 */
public class SearcherStrategyRemediator<T> implements Remediator<T> {

  public static final class Builder<T> {
    private final Map<FixCandidateSearcher<T>, RemediationStrategy> searcherRemediatorMap;

    public Builder() {
      this.searcherRemediatorMap = new HashMap<>();
    }

    public Builder<T> withSearcherStrategyPair(
        final FixCandidateSearcher<T> searcher, final RemediationStrategy strategy) {
      this.searcherRemediatorMap.put(
          Objects.requireNonNull(searcher), Objects.requireNonNull(strategy));
      return this;
    }

    public Builder<T> withFunctions(
        final Predicate<Node> searcherMatcher,
        final BiFunction<CompilationUnit, Node, SuccessOrReason> fixer) {
      this.searcherRemediatorMap.put(
          new FixCandidateSearcher.Builder<T>()
              .withMatcher(Objects.requireNonNull(searcherMatcher))
              .build(),
          new ModularRemediationStrategy(fixer));
      return this;
    }

    public SearcherStrategyRemediator<T> build() {
      return new SearcherStrategyRemediator<>(searcherRemediatorMap);
    }
  }

  private final Map<FixCandidateSearcher<T>, RemediationStrategy> searcherRemediatorMap;

  protected SearcherStrategyRemediator(
      Map<FixCandidateSearcher<T>, RemediationStrategy> searcherRemediatorMap) {
    this.searcherRemediatorMap = searcherRemediatorMap;
  }

  /** Remediate with a chosen searcher-strategy pair. */
  private Pair<List<CodemodChange>, List<UnfixedFinding>> remediateWithStrategy(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingStartLineExtractor,
      final Function<T, Optional<Integer>> findingEndLineExtractor,
      final Function<T, Optional<Integer>> findingColumnExtractor,
      final FixCandidateSearcher<T> searcher,
      final RemediationStrategy strategy) {

    FixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            new ArrayList<>(findingsForPath),
            findingIdExtractor,
            findingStartLineExtractor,
            findingEndLineExtractor,
            findingColumnExtractor);

    if (findingsForPath.isEmpty()) {
      return Pair.with(List.of(), List.of());
    }

    final List<UnfixedFinding> unfixedFindings = new ArrayList<>(results.unfixableFindings());
    final List<CodemodChange> changes = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();
      Integer line = findingStartLineExtractor.apply(issues.get(0));

      if (line == null) {
        issues.forEach(
            issue -> {
              final String id = findingIdExtractor.apply(issue);
              final UnfixedFinding unfixableFinding =
                  new UnfixedFinding(id, detectorRule, path, null, "No line number provided");
              unfixedFindings.add(unfixableFinding);
            });
        continue;
      }

      // Try to fix, gathering the added dependencies if successful
      var maybeDeps = strategy.fix(cu, fixCandidate.node());
      if (maybeDeps.isSuccess()) {
        List<FixedFinding> fixedFindings =
            issues.stream()
                .map(findingIdExtractor)
                .map(findingId -> new FixedFinding(findingId, detectorRule))
                .toList();

        changes.add(CodemodChange.from(line, maybeDeps.getDependencies(), fixedFindings));
      } else {
        issues.forEach(
            issue -> {
              final String id = findingIdExtractor.apply(issue);
              final UnfixedFinding unfixableFinding =
                  new UnfixedFinding(id, detectorRule, path, line, maybeDeps.getReason());
              unfixedFindings.add(unfixableFinding);
            });
      }
    }
    return Pair.with(changes, unfixedFindings);
  }

  public CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingStartLineExtractor,
      final Function<T, Optional<Integer>> findingEndLineExtractor,
      final Function<T, Optional<Integer>> findingColumnExtractor) {

    List<CodemodChange> allChanges = new ArrayList<>();
    List<UnfixedFinding> allUnfixed = new ArrayList<>();

    for (var searcherAndStrategy : searcherRemediatorMap.entrySet()) {
      var pairResult =
          remediateWithStrategy(
              cu,
              path,
              detectorRule,
              findingsForPath,
              findingIdExtractor,
              findingStartLineExtractor,
              findingEndLineExtractor,
              findingColumnExtractor,
              searcherAndStrategy.getKey(),
              searcherAndStrategy.getValue());
      allChanges.addAll(pairResult.getValue0());
      allUnfixed.addAll(pairResult.getValue1());
    }
    return CodemodFileScanningResult.from(allChanges, allUnfixed);
  }
}
