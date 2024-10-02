package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.javadeserialization.JavaDeserializationRemediator;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/** Fixes Object Deserialization issues found by sonsr rule javasecurity:S5135. */
@Codemod(
    id = "sonar:java/object-deserialization-s5135",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SonarObjectDeserializationCodemod extends SonarRemediatingJavaParserChanger {

  private final JavaDeserializationRemediator remediator;
  private final RuleIssue issues;

  @Inject
  public SonarObjectDeserializationCodemod(
      @ProvidedSonarScan(ruleId = "javasecurity:S5135") final RuleIssue issues) {
    super(GenericRemediationMetadata.DESERIALIZATION.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediator = JavaDeserializationRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "javasecurity:S5135",
        "Deserialization should not be vulnerable to injection attacks",
        "https://rules.sonarsource.com/java/RSPEC-5135/");
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
