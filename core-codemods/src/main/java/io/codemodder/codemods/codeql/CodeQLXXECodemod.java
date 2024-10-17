package io.codemodder.codemods.codeql;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.xxe.XXERemediator;
import javax.inject.Inject;

/** A codemod for automatically fixing SQL injection from CodeQL. */
@Codemod(
    id = "codeql:java/xxe",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLXXECodemod extends CodeQLRemediationCodemod {

  private final XXERemediator remediator;

  @Inject
  public CodeQLXXECodemod(@ProvidedCodeQLScan(ruleId = "java/xxe") final RuleSarif sarif) {
    super(GenericRemediationMetadata.XXE.reporter(), sarif);
    this.remediator = XXERemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "xxe",
        "Resolving XML external entity in user-controlled data",
        "https://codeql.github.com/codeql-query-help/java/java-xxe/");
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
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn());
  }
}
