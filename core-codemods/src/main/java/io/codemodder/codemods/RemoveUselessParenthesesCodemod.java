package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** Codemod to remove useless pair of parentheses */
@Codemod(
    id = "sonar:java/remove-useless-parentheses-s1110",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUselessParenthesesCodemod
    extends SonarPluginJavaParserChanger<EnclosedExpr> {

  @Inject
  public RemoveUselessParenthesesCodemod(
      @ProvidedSonarScan(ruleId = "java:S1110") final RuleIssues issues) {
    super(issues, EnclosedExpr.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final EnclosedExpr enclosedExpr,
      final Issue issue) {

    Expression innerExpr = enclosedExpr.getInner();
    enclosedExpr.replace(innerExpr);

    return true;
  }
}
