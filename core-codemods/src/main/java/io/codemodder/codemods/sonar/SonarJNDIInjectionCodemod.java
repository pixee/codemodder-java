package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.jndiinjection.JNDIInjectionRemediator;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

/** This codemod knows how to fix JNDI vulnerabilities found by sonar. */
@Codemod(
    id = "sonar:java/jndi-injection-s2078",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SonarJNDIInjectionCodemod extends SonarRemediatingJavaParserChanger {

  private final RuleIssue issues;
  private final Remediator<Issue> remediator;

  @Inject
  public SonarJNDIInjectionCodemod(
      @ProvidedSonarScan(ruleId = "javasecurity:S2078") final RuleIssue issues) {
    super(GenericRemediationMetadata.JNDI.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediator = new JNDIInjectionRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "javasecurity:S2078",
        "LDAP queries should not be vulnerable to injection attacks",
        "https://rules.sonarsource.com/java/RSPEC-2078/");
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
        i ->
            i.getTextRange() != null
                ? Optional.of(i.getTextRange().getEndLine())
                : Optional.empty(),
        i ->
            i.getTextRange() != null
                ? Optional.of(i.getTextRange().getStartOffset() + 1)
                : Optional.empty());
  }
}
