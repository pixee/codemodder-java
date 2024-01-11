package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for creating a constant for a literal string that is duplicated n times. */
@Codemod(
    id = "sonar:java/define-constant-for-duplicate-literal-s1192",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DefineConstantForLiteralCodemod
    extends SonarPluginJavaParserChanger<StringLiteralExpr> {

  @Inject
  public DefineConstantForLiteralCodemod(
      @ProvidedSonarScan(ruleId = "java:S1192") final RuleIssues issues) {
    super(issues, StringLiteralExpr.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {

    final DefineConstantForLiteral defineConstantForLiteral =
        issue.getMessage().startsWith("Define a constant") ? new CreateConstantForLiteral() : null;

    return defineConstantForLiteral.defineConstant(context, cu, stringLiteralExpr, issue);
  }
}
