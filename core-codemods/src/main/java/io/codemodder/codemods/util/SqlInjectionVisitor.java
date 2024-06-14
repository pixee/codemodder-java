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
public class SqlInjectionVisitor {

  private SqlInjectionVisitor() {}

  public static <T> CodemodFileScanningResult visit(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      Collection<T> findingsForThisPath,
      final DetectorRule detectorRule,
      Function<T, String> idExtractor,
      Function<T, Integer> lineExtractor) {

    List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    if (findingsForThisPath.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    for (T finding : findingsForThisPath) {
      String id = idExtractor.apply(finding);
      Integer line = lineExtractor.apply(finding);

      if (line == null) {
        UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id, detectorRule, context.path().toString(), null, "No line number provided");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      List<MethodCallExpr> supportedSqlMethodCallsOnThatLine =
          allMethodCalls.stream()
              .filter(methodCallExpr -> methodCallExpr.getRange().get().begin.line == line)
              .filter(SQLParameterizer::isSupportedJdbcMethodCall)
              .toList();

      if (supportedSqlMethodCallsOnThatLine.isEmpty()) {
        UnfixedFinding unfixableFinding =
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
        UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                detectorRule,
                context.path().toString(),
                line,
                "Multiple supported SQL methods found on the given line");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      MethodCallExpr methodCallExpr = supportedSqlMethodCallsOnThatLine.get(0);
      if (SQLParameterizerWithCleanup.checkAndFix(methodCallExpr)) {
        changes.add(CodemodChange.from(line, new FixedFinding(id, detectorRule)));
      } else {
        UnfixedFinding unfixableFinding =
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
