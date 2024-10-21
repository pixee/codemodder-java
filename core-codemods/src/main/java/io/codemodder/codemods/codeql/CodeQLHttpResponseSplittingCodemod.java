package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.headerinjection.HeaderInjectionRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing HTTP response splitting from CodeQL. */
@Codemod(
    id = "codeql:java/http-response-splitting",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLHttpResponseSplittingCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLHttpResponseSplittingCodemod(
      @ProvidedCodeQLScan(ruleId = "java/http-response-splitting") final RuleSarif sarif) {
    super(GenericRemediationMetadata.HEADER_INJECTION.reporter(), sarif);
    this.remediator = new HeaderInjectionRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "http-response-splitting",
        "HTTP response splitting",
        "https://codeql.github.com/codeql-query-help/java/java-http-response-splitting/");
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
