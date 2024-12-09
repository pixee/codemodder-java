package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.weakcrypto.WeakCryptoAlgorithmRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing weak crypto algorithms. */
@Codemod(
    id = "codeql:java/potentially-weak-cryptographic-algorithm",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLPotentiallyUnsafeCryptoAlgorithmCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLPotentiallyUnsafeCryptoAlgorithmCodemod(
      @ProvidedCodeQLScan(ruleId = "java/potentially-weak-cryptographic-algorithm")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.WEAK_CRYPTO_ALGORITHM.reporter(), sarif);
    this.remediator = new WeakCryptoAlgorithmRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "potentially-weak-cryptographic-algorithm",
        "Use of a potentially broken or risky cryptographic algorithm",
        "https://codeql.github.com/codeql-query-help/java/java-potentially-weak-cryptographic-algorithm/");
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
        r -> Optional.empty());
  }
}
