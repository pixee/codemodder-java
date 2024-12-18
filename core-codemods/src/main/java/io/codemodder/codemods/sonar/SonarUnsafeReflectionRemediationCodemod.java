package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.reflectioninjection.ReflectionInjectionRemediator;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.TextRange;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

/** Sonar remediation codemod for S2658: Classes should not be loaded dynamically. */
@Codemod(
    id = "sonar:java/unsafe-reflection-s2658",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarUnsafeReflectionRemediationCodemod
    extends SonarRemediatingJavaParserChanger {

  private final Remediator<Issue> remediator;
  private final RuleIssue issues;

  @Inject
  public SonarUnsafeReflectionRemediationCodemod(
      @ProvidedSonarScan(ruleId = "java:S2658") final RuleIssue issues) {
    super(GenericRemediationMetadata.REFLECTION_INJECTION.reporter(), issues);
    this.remediator = new ReflectionInjectionRemediator<>();
    this.issues = Objects.requireNonNull(issues);
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2658",
        "Classes should not be loaded dynamically",
        "https://rules.sonarsource.com/java/RSPEC-2658/");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return remediator.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        issues.getResultsByPath(context.path()),
        Issue::getKey,
        i -> i.getTextRange() != null ? i.getTextRange().getStartLine() : i.getLine(),
        i -> Optional.ofNullable(i.getTextRange()).map(TextRange::getEndLine),
        i -> Optional.empty());
  }
}
