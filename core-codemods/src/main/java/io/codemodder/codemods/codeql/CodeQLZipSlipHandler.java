package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.zipslip.ZipSlipRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing Zip Slip issues from CodeQL. */
@Codemod(
    id = "codeql:java/zipslip",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLZipSlipHandler extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLZipSlipHandler(@ProvidedCodeQLScan(ruleId = "java/zipslip") final RuleSarif sarif) {
    super(GenericRemediationMetadata.ZIP_SLIP.reporter(), sarif);
    this.remediator = new ZipSlipRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "zipslip",
        "Arbitrary file access during archive extraction (\"Zip Slip\")\n",
        "https://codeql.github.com/codeql-query-help/java/java-zipslip/");
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
        r -> Optional.of(r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
        r ->
            Optional.of(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn()));
  }
}
