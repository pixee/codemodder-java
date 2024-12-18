package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.weakrandom.WeakRandomRemediator;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/weak-prng-2245",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarWeakRandomCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Hotspot> remediationStrategy;
  private final RuleHotspot issues;

  @Inject
  public SonarWeakRandomCodemod(
      @ProvidedSonarScan(ruleId = "java:S2245") final RuleHotspot hotspots) {
    super(GenericRemediationMetadata.WEAK_RANDOM.reporter(), hotspots);
    this.issues = Objects.requireNonNull(hotspots);
    this.remediationStrategy = new WeakRandomRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2245",
        "Make sure that using this pseudorandom number generator is safe here",
        "https://rules.sonarsource.com/java/RSPEC-2245/?search=weak%20random");
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
