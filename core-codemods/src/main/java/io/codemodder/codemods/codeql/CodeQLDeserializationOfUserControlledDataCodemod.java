package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.javadeserialization.JavaDeserializationRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing untrusted deserialization from CodeQL. */
@Codemod(
    id = "codeql:java/unsafe-deserialization",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLDeserializationOfUserControlledDataCodemod
    extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLDeserializationOfUserControlledDataCodemod(
      @ProvidedCodeQLScan(ruleId = "java/unsafe-deserialization") final RuleSarif sarif) {
    super(GenericRemediationMetadata.DESERIALIZATION.reporter(), sarif);
    this.remediator = new JavaDeserializationRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "unsafe-deserialization",
        "Deserialization of user-controlled data",
        "https://codeql.github.com/codeql-query-help/java/java-unsafe-deserialization/");
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
