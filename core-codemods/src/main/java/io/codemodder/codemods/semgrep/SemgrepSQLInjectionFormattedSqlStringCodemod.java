package io.codemodder.codemods.semgrep;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.SarifFindingKeyUtil;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.semgrep.ProvidedSemgrepScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.sqlinjection.JavaParserSQLInjectionRemediatorStrategy;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id
 * "java.lang.security.audit.formatted-sql-string.formatted-sql-string".
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.formatted-sql-string.formatted-sql-string",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SemgrepSQLInjectionFormattedSqlStringCodemod extends SemgrepJavaParserChanger {

  private final JavaParserSQLInjectionRemediatorStrategy remediator;

  @Inject
  public SemgrepSQLInjectionFormattedSqlStringCodemod(
      @ProvidedSemgrepScan(
              ruleId = "java.lang.security.audit.formatted-sql-string.formatted-sql-string")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), sarif);
    this.remediator = JavaParserSQLInjectionRemediatorStrategy.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "SQL Injection",
        "https://semgrep.dev/playground/r/java.lang.security.audit.formatted-sql-string.formatted-sql-string");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return remediator.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        ruleSarif.getResultsByLocationPath(context.path()),
        SarifFindingKeyUtil::buildFindingId,
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine(),
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine());
  }
}
