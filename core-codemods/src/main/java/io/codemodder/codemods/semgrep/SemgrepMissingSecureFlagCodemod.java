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
import io.codemodder.remediation.missingsecureflag.MissingSecureFlagRemediator;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id
 * "java.lang.security.audit.cookie-missing-secure-flag.cookie-missing-secure-flag"
 */
@Codemod(
    id =
        "semgrep:java/java.lang.security.audit.cookie-missing-secure-flag.cookie-missing-secure-flag",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SemgrepMissingSecureFlagCodemod extends SemgrepJavaParserChanger {

  private final MissingSecureFlagRemediator remediator;

  @Inject
  public SemgrepMissingSecureFlagCodemod(
      @ProvidedSemgrepScan(
              ruleId =
                  "java.lang.security.audit.cookie-missing-secure-flag.cookie-missing-secure-flag")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.MISSING_SECURE_FLAG.reporter(), sarif);
    this.remediator = MissingSecureFlagRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "Missing Secure Flag",
        "https://semgrep.dev/playground/r/NdT3dLr/java.lang.security.audit.cookie-missing-secure-flag.cookie-missing-secure-flag");
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
