package io.codemodder.codemods.util;

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
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Default implementation of the JavaParserSQLInjectionRemediatorStrategy interface. This class
 * provides the logic to visit a CompilationUnit and process findings for potential SQL injections.
 */
public final class ParameterizedJavaParserSQLInjectionRemediatorStrategy
    implements JavaParserSQLInjectionRemediatorStrategy {

  private Predicate<MethodCallExpr> matcher;
  private Predicate<MethodCallExpr> fixer;

  public ParameterizedJavaParserSQLInjectionRemediatorStrategy(
      final Predicate<MethodCallExpr> matcher, final Predicate<MethodCallExpr> fixer) {
    this.matcher = matcher;
    this.fixer = fixer;
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
      return CodemodFileScanningResult.none();
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

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
