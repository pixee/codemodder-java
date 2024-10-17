package io.codemodder.codemods.codeql;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codemods.remediators.weakrandom.WeakRandomRemediator;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import javax.inject.Inject;

/** A codemod for automatically fixing insecure randomness from CodeQL. */
@Codemod(
    id = "codeql:java/insecure-randomness",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLInsecureRandomnessCodemod extends CodeQLRemediationCodemod {

  private final WeakRandomRemediator remediator;

  @Inject
  public CodeQLInsecureRandomnessCodemod(
      @ProvidedCodeQLScan(ruleId = "java/insecure-randomness") final RuleSarif sarif) {
    super(GenericRemediationMetadata.WEAK_RANDOM.reporter(), sarif);
    this.remediator = WeakRandomRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "insecure-randomness",
        "Insecure randomness",
        "https://codeql.github.com/codeql-query-help/java/java-insecure-randomness/");
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
