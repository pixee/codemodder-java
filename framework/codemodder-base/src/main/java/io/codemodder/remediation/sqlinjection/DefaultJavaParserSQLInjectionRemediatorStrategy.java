package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.javatuples.Pair;

/**
 * Default implementation of the JavaParserSQLInjectionRemediatorStrategy interface. This class
 * provides the logic to visit a CompilationUnit and process findings for potential SQL injections.
 */
final class DefaultJavaParserSQLInjectionRemediatorStrategy
    implements JavaParserSQLInjectionRemediatorStrategy {

  private final Map<Predicate<MethodCallExpr>, Predicate<MethodCallExpr>> remediationStrategies;

  /**
   * Builds a strategy from a matcher-fixer pair. A matcher is a predicate that matches the call,
   * ensure it is the right one for attempting a fix. A fixer fixes a predicate that chagnes the AST
   * as a side-effect and reports if successful with a boolean.
   */
  DefaultJavaParserSQLInjectionRemediatorStrategy(
      final Predicate<MethodCallExpr> matcher, final Predicate<MethodCallExpr> fixer) {
    this.remediationStrategies = Map.of(matcher, fixer);
  }

  /** Builds a grand strategy as a combination of several strategies. */
  DefaultJavaParserSQLInjectionRemediatorStrategy(
      final Map<Predicate<MethodCallExpr>, Predicate<MethodCallExpr>> strategies) {
    this.remediationStrategies = strategies;
  }

  /** Remediate with a chosen strategy. */
  private <T> Pair<List<CodemodChange>, List<UnfixedFinding>> remediateWithStrategy(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingLineExtractor,
      final Predicate<MethodCallExpr> matcher,
      final Predicate<MethodCallExpr> fixer) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>().withMatcher(matcher).build();

    FixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            new ArrayList<>(findingsForPath),
            findingIdExtractor,
            findingLineExtractor,
            f -> null);

    if (findingsForPath.isEmpty()) {
      return Pair.with(List.of(), List.of());
    }

    final List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    final List<CodemodChange> changes = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();
      Integer line = findingLineExtractor.apply(issues.get(0));

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

      final MethodCallExpr methodCallExpr = fixCandidate.methodCall();
      if (fixer.test(methodCallExpr)) {
        issues.forEach(
            issue -> {
              final String id = findingIdExtractor.apply(issue);
              changes.add(CodemodChange.from(line, new FixedFinding(id, detectorRule)));
            });
      } else {
        issues.forEach(
            issue -> {
              final String id = findingIdExtractor.apply(issue);
              final UnfixedFinding unfixableFinding =
                  new UnfixedFinding(
                      id,
                      detectorRule,
                      path,
                      line,
                      "State changing effects possible or unrecognized code shape");
              unfixedFindings.add(unfixableFinding);
            });
      }
    }
    return Pair.with(changes, unfixedFindings);
  }

  /**
   * Visits the provided CompilationUnit and processes findings for potential SQL injections.
   *
   * @param cu the compilation unit to be scanned
   * @param path the path of the file being scanned
   * @param detectorRule the detector rule that generated the findings
   * @param findingsForPath a collection of findings to be processed
   * @param findingIdExtractor a function to extract the ID from a finding
   * @param findingLineExtractor a function to extract the line number from a finding
   * @param <T> the type of the findings
   * @return a result object containing the changes and unfixed findings
   */
  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingLineExtractor) {
    List<CodemodChange> allChanges = new ArrayList<>();
    List<UnfixedFinding> allUnfixed = new ArrayList<>();

    for (var matcher : remediationStrategies.keySet()) {
      var fixer = remediationStrategies.get(matcher);
      var pairResult =
          remediateWithStrategy(
              cu,
              path,
              detectorRule,
              findingsForPath,
              findingIdExtractor,
              findingLineExtractor,
              matcher,
              fixer);
      allChanges.addAll(pairResult.getValue0());
      allUnfixed.addAll(pairResult.getValue1());
    }
    return CodemodFileScanningResult.from(allChanges, allUnfixed);
  }
}
