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
import io.codemodder.remediation.javadeserialization.JavaDeserializationRemediator;
import java.util.Optional;
import javax.inject.Inject;

/**
 * Fixes some Semgrep issues reported under the id
 * "java.lang.security.audit.object-deserialization.object-deserialization".
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.object-deserialization.object-deserialization",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SemgrepJavaDeserializationCodemod extends SemgrepJavaParserChanger {

  private final Remediator<Result> remediator;

  @Inject
  public SemgrepJavaDeserializationCodemod(
      @ProvidedSemgrepScan(
              ruleId = "java.lang.security.audit.object-deserialization.object-deserialization")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.DESERIALIZATION.reporter(), sarif);
    this.remediator = new JavaDeserializationRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "Insecure Deserialization",
        "https://semgrep.dev/playground/r/java.lang.security.audit.object-deserialization.object-deserialization");
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
