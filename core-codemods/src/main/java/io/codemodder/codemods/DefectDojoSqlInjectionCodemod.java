package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
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
  private final FixOnlyCodeChanger fixOnlyCodeChangerInformation;

  @Inject
  public DefectDojoSqlInjectionCodemod(
      @DefectDojoScan(ruleId = "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli")
          RuleFindings findings) {
    this.findings = Objects.requireNonNull(findings);
    this.fixOnlyCodeChangerInformation =
        new DefaultFixOnlyCodeChanger(
            "DefectDojo / Semgrep",
            new DetectorRule(
                "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
                "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
                "https://semgrep.dev/r?q=java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli"));
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {

    List<MethodCallExpr> allMethodCalls = cu.findAll(MethodCallExpr.class);

    List<Finding> findingsForThisPath = findings.getForPath(context.path());
    if (findingsForThisPath.isEmpty()) {
      return CodemodFileScanningResult.none();
    }

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    List<CodemodChange> changes = new ArrayList<>();
    for (Finding finding : findingsForThisPath) {
      String id = String.valueOf(finding.getId());
      Integer line = finding.getLine();
      if (line == null) {
        UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                fixOnlyCodeChangerInformation.detectorRule(),
                context.path().toString(),
                null,
                "No line number provided");
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
                fixOnlyCodeChangerInformation.detectorRule(),
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
                fixOnlyCodeChangerInformation.detectorRule(),
                context.path().toString(),
                line,
                "Multiple supported SQL methods found on the given line");
        unfixedFindings.add(unfixableFinding);
        continue;
      }

      MethodCallExpr methodCallExpr = supportedSqlMethodCallsOnThatLine.get(0);
      if (SQLParameterizerWithCleanup.checkAndFix(methodCallExpr)) {
        changes.add(
            CodemodChange.from(
                line, new FixedFinding(id, fixOnlyCodeChangerInformation.detectorRule())));
      } else {
        UnfixedFinding unfixableFinding =
            new UnfixedFinding(
                id,
                fixOnlyCodeChangerInformation.detectorRule(),
                context.path().toString(),
                line,
                "State changing effects possible or unrecognized code shape");
        unfixedFindings.add(unfixableFinding);
      }
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
