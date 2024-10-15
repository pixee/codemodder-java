package io.codemodder.codemods.semgrep;

import com.contrastsecurity.sarif.Result;
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
import io.codemodder.remediation.sqlinjection.SQLInjectionRemediator;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id
 * "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli".
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SemgrepSQLInjectionCodemod extends SemgrepJavaParserChanger {

  private final SQLInjectionRemediator<Result> remediator;

  @Inject
  public SemgrepSQLInjectionCodemod(
      @ProvidedSemgrepScan(ruleId = "java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), sarif);
    this.remediator = new SQLInjectionRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "SQL Injection",
        "https://semgrep.dev/playground/r/java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli");
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
        r ->
            Optional.ofNullable(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
        r ->
            Optional.ofNullable(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn()));
  }
}
