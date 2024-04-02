package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.codetf.DetectionTool;
import io.codemodder.codetf.DetectorFinding;
import io.codemodder.codetf.DetectorRule;
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
public final class DefectDojoSqlInjectionCodemod extends JavaParserChanger
    implements FixOnlyCodeChanger {

  private final RuleFindings findings;

  @Inject
  public DefectDojoSqlInjectionCodemod(
      @DefectDojoScan(ruleId = "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli")
          RuleFindings findings) {
    this.findings = Objects.requireNonNull(findings);
  }

  @Override
  public DetectionTool getDetectionTool() {
    DetectorRule semgrepSqliRule =
        new DetectorRule(
            "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
            "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
            null);
    return new DetectionTool("DefectDojo", semgrepSqliRule, List.of());
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {

    List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    List<Finding> findingsForThisPath = findings.getForPath(context.path());
    if (findingsForThisPath.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    List<DetectorFinding> allFindings = new ArrayList<>();

    List<CodemodChange> changes = new ArrayList<>();
    for (Finding finding : findingsForThisPath) {
      String id = String.valueOf(finding.getId());
      Integer line = finding.getLine();
      if (line == null) {
        DetectorFinding unfixableFinding =
            new DetectorFinding(id, false, "No line number provided");
        allFindings.add(unfixableFinding);
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
        allFindings.add(unfixableFinding);
        continue;
      }

      if (supportedSqlMethodCallsOnThatLine.size() > 1) {
        DetectorFinding unfixableFinding =
            new DetectorFinding(
                id, false, "Multiple supported SQL methods found on the given line");
        allFindings.add(unfixableFinding);
        continue;
      }

      MethodCallExpr methodCallExpr = supportedSqlMethodCallsOnThatLine.get(0);
      SQLParameterizer parameterizer = new SQLParameterizer(methodCallExpr);

      if (parameterizer.checkAndFix()) {
        var maybeMethodDecl = methodCallExpr.findAncestor(CallableDeclaration.class);
        // Cleanup, removes empty string concatenations and unused variables
        maybeMethodDecl.ifPresent(cd -> ASTTransforms.removeEmptyStringConcatenation(cd));

        DetectorFinding fixedFinding = new DetectorFinding(id, true, null);
        allFindings.add(fixedFinding);
        changes.add(CodemodChange.from(line, "Fixes issue " + id + " by parameterizing SQL"));
      } else {
        DetectorFinding unfixableFinding =
            new DetectorFinding(id, false, "Fixing may have side effects");
        allFindings.add(unfixableFinding);
      }
    }

    return CodemodFileScanningResult.from(changes, allFindings);
  }
}
