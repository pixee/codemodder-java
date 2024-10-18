package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.predictableseed.PredictableSeedRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing predictable seeds reported by CodeQL. */
@Codemod(
    id = "codeql:java/predictable-seed",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLPredictableSeedCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLPredictableSeedCodemod(
      @ProvidedCodeQLScan(ruleId = "java/predictable-seed") final RuleSarif sarif) {
    super(GenericRemediationMetadata.PREDICTABLE_SEED.reporter(), sarif);
    this.remediator = new PredictableSeedRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "predictable-seed",
        "Use of a predictable seed in a secure random number generator",
        "https://codeql.github.com/codeql-query-help/java/java-predictable-seed/");
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
        result -> result.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine(),
        result ->
            Optional.ofNullable(
                result.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
        result ->
            Optional.ofNullable(
                result.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn()));
  }
}
