package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.ssrf.SSRFRemediator;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import io.codemodder.sonar.model.TextRange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

/** Fixes SSRF issues found by sonar rule javasecurity:S5144. */
@Codemod(
    id = "sonar:java/ssrf-s5144",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SonarSSRFCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Issue> remediator;
  private final RuleIssue issues;

  @Inject
  public SonarSSRFCodemod(
      @ProvidedSonarScan(ruleId = "javasecurity:S5144") final RuleIssue issues) {
    super(GenericRemediationMetadata.SSRF.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediator = new SSRFRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "javasecurity:S5144",
        "Server-side requests should not be vulnerable to forging attacks",
        "https://rules.sonarsource.com/java/RSPEC-5144/");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Issue> issuesForFile = issues.getResultsByPath(context.path());
    return remediator.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        issuesForFile,
        SonarFinding::getKey,
        i -> i.getTextRange() != null ? i.getTextRange().getStartLine() : i.getLine(),
        i -> Optional.ofNullable(i.getTextRange()).map(TextRange::getEndLine),
        i -> Optional.ofNullable(i.getTextRange()).map(tr -> tr.getStartOffset() + 1));
  }
}
