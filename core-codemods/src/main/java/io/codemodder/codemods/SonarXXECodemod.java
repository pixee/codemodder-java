package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.xxe.XXERemediator;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/xxe-2755",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarXXECodemod extends SonarRemediatingJavaParserChanger {

  private final XXERemediator remediationStrategy;
  private final RuleIssue issues;

  @Inject
  public SonarXXECodemod(@ProvidedSonarScan(ruleId = "java:S2755") final RuleIssue issues) {
    super(GenericRemediationMetadata.XXE.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediationStrategy = XXERemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2755",
        "XML parsers should not be vulnerable to XXE attacks",
        "https://rules.sonarsource.com/c/type/Vulnerability/RSPEC-2755/");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Issue> issuesForFile = issues.getResultsByPath(context.path());
    return remediationStrategy.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        issuesForFile,
        SonarFinding::getKey,
        SonarFinding::getLine,
        f -> f.getTextRange().getStartOffset());
  }
}
