package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.weakcrypto.WeakCryptoAlgorithmRemediator;
import io.codemodder.remediation.weakrandom.WeakRandomRemediator;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Codemod(
    id = "sonar:java/weak-hash-2245",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarWeakHashingAlgorithmCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Hotspot> remediationStrategy;
  private final RuleHotspot issues;

  @Inject
  public SonarWeakHashingAlgorithmCodemod(
      @ProvidedSonarScan(ruleId = "java:S4790") final RuleHotspot hotspots) {
    super(GenericRemediationMetadata.WEAK_CRYPTO_ALGORITHM.reporter(), hotspots);
    this.issues = Objects.requireNonNull(hotspots);
    this.remediationStrategy = new WeakCryptoAlgorithmRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S4790",
        "Using weak hashing algorithms is security-sensitive",
        "https://rules.sonarsource.com/java/type/Security%20Hotspot/RSPEC-4790/");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Hotspot> issuesForFile = issues.getResultsByPath(context.path());
    return remediationStrategy.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        issuesForFile,
        SonarFinding::getKey,
        i -> i.getTextRange() != null ? i.getTextRange().getStartLine() : i.getLine(),
        i ->
            i.getTextRange() != null
                ? Optional.of(i.getTextRange().getEndLine())
                : Optional.empty(),
        i -> Optional.empty());
  }
}
