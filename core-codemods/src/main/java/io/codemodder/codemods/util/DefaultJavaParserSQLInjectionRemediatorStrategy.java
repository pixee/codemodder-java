package io.codemodder.codemods.util;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.codemods.SQLParameterizer;
import io.codemodder.codemods.SQLParameterizerWithCleanup;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Utility class for visiting and processing potential SQL injection points in Java source code. */
final class DefaultJavaParserSQLInjectionRemediatorStrategy
    implements JavaParserSQLInjectionRemediatorStrategy {

  /**
   * Visits the provided CompilationUnit and processes findings for potential SQL injections.
   *
   * @param context the context of the codemod invocation
   * @param cu the compilation unit to be scanned
   * @param pathFindings a collection of findings to be processed
   * @param detectorRule the rule used to detect potential issues
   * @param findingIdExtractor a function to extract the ID from a finding
   * @param findingLineExtractor a function to extract the line number from a finding
   * @param <T> the type of the findings
   * @return a result object containing the changes and unfixed findings
   */
  public <T> CodemodFileScanningResult visit(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Collection<T> pathFindings,
      final DetectorRule detectorRule,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingLineExtractor) {

    final List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    if (pathFindings.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    final List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    final List<CodemodChange> changes = new ArrayList<>();

    for (T finding : pathFindings) {
      final String id = findingIdExtractor.apply(finding);
      final Integer line = findingLineExtractor.apply(finding);

      if (line == null) {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id, detectorRule, context.path().toString(), null, "No line number provided");
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
                id,
                detectorRule,
                context.path().toString(),
                line,
                "No supported SQL methods found on the given line");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      if (supportedSqlMethodCallsOnThatLine.size() > 1) {
        final UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                detectorRule,
                context.path().toString(),
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
                context.path().toString(),
                line,
                "State changing effects possible or unrecognized code shape");
        unfixedFindings.add(unfixableFinding);
      }
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
