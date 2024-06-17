package io.codemodder.codemods.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codemods.SQLParameterizer;
import io.codemodder.codemods.SQLParameterizerWithCleanup;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Default implementation of the JavaParserSQLInjectionRemediatorStrategy interface. This class
 * provides the logic to visit a CompilationUnit and process findings for potential SQL injections.
 */
final class DefaultJavaParserSQLInjectionRemediatorStrategy
    implements JavaParserSQLInjectionRemediatorStrategy {

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

    final List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    if (findingsForPath.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    final List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    final List<CodemodChange> changes = new ArrayList<>();

    for (T finding : findingsForPath) {
      final String id = findingIdExtractor.apply(finding);
      final Integer line = findingLineExtractor.apply(finding);

      if (line == null) {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(id, detectorRule, path, null, "No line number provided");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      final List<MethodCallExpr> supportedSqlMethodCallsOnThatLine =
          allMethodCalls.stream()
              .filter(methodCallExpr -> methodCallExpr.getRange().get().begin.line == line)
              .filter(SQLParameterizer::isSupportedJdbcMethodCall)
              .toList();

      if (supportedSqlMethodCallsOnThatLine.isEmpty()) {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id, detectorRule, path, line, "No supported SQL methods found on the given line");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      if (supportedSqlMethodCallsOnThatLine.size() > 1) {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                detectorRule,
                path,
                line,
                "Multiple supported SQL methods found on the given line");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      final MethodCallExpr methodCallExpr = supportedSqlMethodCallsOnThatLine.get(0);
      if (SQLParameterizerWithCleanup.checkAndFix(methodCallExpr)) {
        changes.add(CodemodChange.from(line, new FixedFinding(id, detectorRule)));
      } else {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                detectorRule,
                path,
                line,
                "State changing effects possible or unrecognized code shape");
        unfixedFindings.add(unfixableFinding);
      }
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
