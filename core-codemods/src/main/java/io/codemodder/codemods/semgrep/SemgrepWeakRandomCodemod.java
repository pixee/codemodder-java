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
import io.codemodder.remediation.weakrandom.WeakRandomRemediator;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id
 * "java.lang.security.audit.crypto.weak-random.weak-random" (not all cases.)
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.crypto.weak-random.weak-random",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.MEDIUM)
public final class SemgrepWeakRandomCodemod extends SemgrepJavaParserChanger {

  private final WeakRandomRemediator remediator;

  @Inject
  public SemgrepWeakRandomCodemod(
      @ProvidedSemgrepScan(ruleId = "java.lang.security.audit.crypto.weak-random.weak-random")
          final RuleSarif sarif) {
    super(io.codemodder.remediation.GenericRemediationMetadata.WEAK_RANDOM.reporter(), sarif);
    this.remediator = WeakRandomRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "Weak Random",
        "https://semgrep.dev/playground/r/NdT3dLr/java.lang.security.audit.crypto.weak-random.weak-random");
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
