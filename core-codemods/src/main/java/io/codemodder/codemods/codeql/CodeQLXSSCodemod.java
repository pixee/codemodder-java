package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.xss.XSSRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing XSS from CodeQL. */
@Codemod(
    id = "codeql:java/xss",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLXSSCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLXSSCodemod(@ProvidedCodeQLScan(ruleId = "java/xss") final RuleSarif sarif) {
    super(GenericRemediationMetadata.XSS.reporter(), sarif);
    this.remediator = new XSSRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "xss",
        "Cross-site scripting",
        "https://codeql.github.com/codeql-query-help/java/java-xss/");
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
