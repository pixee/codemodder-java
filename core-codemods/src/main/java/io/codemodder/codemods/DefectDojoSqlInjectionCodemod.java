package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.providers.defectdojo.DefectDojoScan;
import io.codemodder.providers.defectdojo.Finding;
import io.codemodder.providers.defectdojo.RuleFindings;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.sqlinjection.SQLInjectionRemediator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

/**
 * This codemod knows how to fix SQL injection findings that come through DefectDojo for supported
 * vendors.
 */
@Codemod(
    id = "defectdojo:java/sql-injection",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class DefectDojoSqlInjectionCodemod extends JavaParserChanger
    implements FixOnlyCodeChanger {

  private final RuleFindings findings;
  private final Remediator<Finding> remediatorStrategy;

  @Inject
  public DefectDojoSqlInjectionCodemod(
      @DefectDojoScan(ruleId = "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli")
          RuleFindings findings) {
    super(CodemodReporterStrategy.fromClasspath(SQLParameterizerCodemod.class));
    this.findings = Objects.requireNonNull(findings);
    this.remediatorStrategy = new SQLInjectionRemediator<>();
  }

  @Override
  public String vendorName() {
    return "DefectDojo / Semgrep";
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
        "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
        "https://semgrep.dev/r?q=java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Finding> findingsForThisPath = findings.getForPath(context.path());
    return remediatorStrategy.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        findingsForThisPath,
        finding -> String.valueOf(finding.getId()),
        Finding::getLine,
        f -> Optional.empty(),
        f -> Optional.empty());
  }
}
