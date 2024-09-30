package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codemods.remediators.openredirect.OpenRedirectRemediator;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/** Fixes Open Redirect issues found by sonsr rule javasecurity:S5146. */
@Codemod(
    id = "sonar:java/open-redirect-s5146",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SonarOpenRedirectCodemod extends SonarRemediatingJavaParserChanger {

  private final OpenRedirectRemediator remediator;
  private final RuleIssue issues;

  @Inject
  public SonarOpenRedirectCodemod(
      @ProvidedSonarScan(ruleId = "javasecurity:S5146") final RuleIssue issues) {
    super(GenericRemediationMetadata.SSRF.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediator = OpenRedirectRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "javasecurity:S5146",
        "HTTP request redirections should not be open to forging attacks",
        "https://rules.sonarsource.com/java/RSPEC-5146/");
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
        i -> i.getTextRange() != null ? i.getTextRange().getEndLine() : null,
        i -> i.getTextRange() != null ? i.getTextRange().getStartOffset() : null);
  }
}
