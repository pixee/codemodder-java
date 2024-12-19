package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.loginjection.LogInjectionRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing Log Injection from CodeQL. */
@Codemod(
    id = "codeql:java/log-injection",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.LOW)
public final class CodeQLLogInjectionCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLLogInjectionCodemod(
      @ProvidedCodeQLScan(ruleId = "java/log-injection") final RuleSarif sarif) {
    super(GenericRemediationMetadata.LOG_INJECTION.reporter(), sarif);
    this.remediator = new LogInjectionRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "log-injection",
        "Log Injection",
        "https://codeql.github.com/codeql-query-help/java/java-log-injection/");
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
        r -> Optional.empty());
  }
}
