package io.codemodder.codemods.codeql;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codemods.remediators.ssrf.SSRFRemediator;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import javax.inject.Inject;

/** A codemod for automatically fixing SQL injection from CodeQL. */
@Codemod(
    id = "codeql:java/ssrf",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLSSRFCodemod extends CodeQLRemediationCodemod {

  private final SSRFRemediator remediator;

  @Inject
  public CodeQLSSRFCodemod(@ProvidedCodeQLScan(ruleId = "java/ssrf") final RuleSarif sarif) {
    super(GenericRemediationMetadata.SSRF.reporter(), sarif);
    this.remediator = SSRFRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "ssrf",
        "Server-side request forgery",
        "https://codeql.github.com/codeql-query-help/java/java-ssrf/");
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
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine(),
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn());
  }
}
