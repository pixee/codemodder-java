package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.errorexposure.ErrorMessageExposureRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing SQL injection from CodeQL. */
@Codemod(
    id = "codeql:java/error-message-exposure",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLErrorMessageExposureCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLErrorMessageExposureCodemod(
      @ProvidedCodeQLScan(ruleId = "java/error-message-exposure") final RuleSarif sarif) {
    super(GenericRemediationMetadata.ERROR_MESSAGE_EXPOSURE.reporter(), sarif);
    this.remediator = new ErrorMessageExposureRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "error-message-exposure",
        "Information exposure through an error message",
        "https://codeql.github.com/codeql-query-help/java/java-error-message-exposure/");
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
