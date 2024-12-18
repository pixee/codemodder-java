package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import javax.inject.Inject;

/** Codemod to remove useless pair of parentheses */
@Codemod(
    id = "sonar:java/remove-useless-parentheses-s1110",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUselessParenthesesCodemod
    extends SonarPluginJavaParserChanger<EnclosedExpr, Issue> {

  @Inject
  public RemoveUselessParenthesesCodemod(
      @ProvidedSonarScan(ruleId = "java:S1110") final RuleIssue issues) {
    super(issues, EnclosedExpr.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final EnclosedExpr enclosedExpr,
      final Issue issue) {

    Expression innerExpr = enclosedExpr.getInner();
    enclosedExpr.replace(innerExpr);

    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1110",
        "Redundant pairs of parentheses should be removed",
        "https://rules.sonarsource.com/java/RSPEC-1110");
  }
}
