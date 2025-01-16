package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.sqlinjection.SQLInjectionFixComposer;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import io.codemodder.sonar.model.TextRange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/sql-injection-s3649",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarSQLInjectionIssueCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Issue> remediationStrategy;
  private final RuleIssue issues;

  @Inject
  public SonarSQLInjectionIssueCodemod(
      @ProvidedSonarScan(ruleId = "javasecurity:S3649") final RuleIssue issues) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), issues);
    this.issues = Objects.requireNonNull(issues);
    this.remediationStrategy =
        new SearcherStrategyRemediator.Builder<Issue>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Issue>()
                    .withMatcher(
                        n ->
                            Optional.empty()
                                // is the argument of the call
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(
                                                m ->
                                                    m instanceof Expression ? (Expression) m : null)
                                            .flatMap(ASTs::isArgumentOfMethodCall)
                                            .filter(SQLInjectionFixComposer::match))
                                .isPresent())
                    .build(),
                new SQLInjectionFixComposer())
            .build();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "javasecurity:S3649",
        "Database queries should not be vulnerable to injection attacks",
        "https://rules.sonarsource.com/java/RSPEC-3649/");
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
        i -> i.getTextRange() != null ? i.getTextRange().getStartLine() : i.getLine(),
        i -> Optional.ofNullable(i.getTextRange()).map(TextRange::getEndLine),
        i -> Optional.ofNullable(i.getTextRange()).map(tr -> tr.getStartOffset() + 1));
  }
}
