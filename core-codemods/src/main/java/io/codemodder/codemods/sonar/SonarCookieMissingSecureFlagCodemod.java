package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.missingsecureflag.MissingSecureFlagRemediator;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/cookie-missing-secure-flag-2092",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarCookieMissingSecureFlagCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Hotspot> remediationStrategy;
  private final RuleHotspot issues;

  @Inject
  public SonarCookieMissingSecureFlagCodemod(
      @ProvidedSonarScan(ruleId = "java:S2092") final RuleHotspot hotspots) {
    super(GenericRemediationMetadata.MISSING_SECURE_FLAG.reporter(), hotspots);
    this.issues = Objects.requireNonNull(hotspots);
    this.remediationStrategy = new MissingSecureFlagRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2092",
        "Make sure creating this cookie without the \"secure\" flag is safe here.",
        "https://rules.sonarsource.com/java/type/Security%20Hotspot/RSPEC-2092/");
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
