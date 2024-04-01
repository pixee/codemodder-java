package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorFinding;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.providers.defectdojo.DefectDojoScan;
import io.codemodder.providers.defectdojo.Finding;
import io.codemodder.providers.defectdojo.RuleFindings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/** This codemod knows how to translate */
@Codemod(
    id = "defectdojo:java/sql-injection",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class DefectDojoSqlInjectionCodemod extends JavaParserChanger {

  private final RuleFindings findings;

  @Inject
  public DefectDojoSqlInjectionCodemod(
      @DefectDojoScan(ruleId = "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli")
          RuleFindings findings) {
    this.findings = Objects.requireNonNull(findings);
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {

    List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    List<Finding> findingsForThisPath = findings.getForPath(context.path());
    if (findingsForThisPath.isEmpty()) {
      return List.of();
    }

    List<DetectorFinding> fixed = new ArrayList<>();
    List<DetectorFinding> allUnfixable = new ArrayList<>();

    List<CodemodChange> changes = new ArrayList<>();
    for (Finding finding : findingsForThisPath) {
      String id = String.valueOf(finding.getId());
      Integer line = finding.getLine();
      if (line == null) {
        DetectorFinding unfixableFinding =
            new DetectorFinding(id, false, "No line number provided");
        allUnfixable.add(unfixableFinding);
        continue;
      }

      List<MethodCallExpr> supportedSqlMethodCallsOnThatLine =
          allMethodCalls.stream()
              .filter(methodCallExpr -> methodCallExpr.getRange().get().begin.line == line)
              .filter(SQLParameterizer::isSupportedJdbcMethodCall)
              .toList();

      if (supportedSqlMethodCallsOnThatLine.isEmpty()) {
        DetectorFinding unfixableFinding =
            new DetectorFinding(id, false, "No supported SQL methods found on the given line");
        allUnfixable.add(unfixableFinding);
        continue;
      }

      if (supportedSqlMethodCallsOnThatLine.size() > 1) {
        DetectorFinding unfixableFinding =
            new DetectorFinding(
                id, false, "Multiple supported SQL methods found on the given line");
        allUnfixable.add(unfixableFinding);
        continue;
      }

      MethodCallExpr methodCallExpr = supportedSqlMethodCallsOnThatLine.get(0);
      SQLParameterizer parameterizer = new SQLParameterizer(methodCallExpr);

      if (parameterizer.checkAndFix()) {
        DetectorFinding fixedFinding = new DetectorFinding(id, true, null);
        fixed.add(fixedFinding);
        changes.add(CodemodChange.from(line, "Fixes issue " + id + " by parameterizing SQL"));
      } else {
        DetectorFinding unfixableFinding =
            new DetectorFinding(id, false, "Fixing may have side effects");
        allUnfixable.add(unfixableFinding);
      }
    }

    return List.copyOf(changes);
  }
}
