package io.codemodder.codemods.semgrep;

import com.contrastsecurity.sarif.Result;
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
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.xss.XSSRemediator;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id "java.lang.security.servletresponse-writer-xss"
 * (not all cases.)
 */
@Codemod(
    id = "semgrep:java/java.lang.security.servletresponse-writer-xss.servletresponse-writer-xss",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.MEDIUM)
public final class SemgrepServletResponseWriterXSSCodemod extends SemgrepJavaParserChanger {

  private final Remediator<Result> remediator;

  @Inject
  public SemgrepServletResponseWriterXSSCodemod(
      @ProvidedSemgrepScan(
              ruleId = "java.lang.security.servletresponse-writer-xss.servletresponse-writer-xss")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.XSS.reporter(), sarif);
    this.remediator = new XSSRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "Cross-Site-Scripting (XSS)",
        "https://semgrep.dev/playground/r/java.lang.security.servletresponse-writer-xss.servletresponse-writer-xss");
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
