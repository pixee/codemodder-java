package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for defining a constant for a literal string that is duplicated n times. */
@Codemod(
    id = "sonar:java/define-constant-for-duplicate-literal-s1192",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
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

    DefineConstantForLiteral defineConstantForLiteral;

    if (issue.getMessage().startsWith("Use already-defined constant")) {
      defineConstantForLiteral =
          new UseExistingConstantForLiteral(context, cu, stringLiteralExpr, issue);
    } else {
      defineConstantForLiteral =
          new CreateConstantForLiteral(context, cu, stringLiteralExpr, issue);
    }

    return defineConstantForLiteral.replaceLiteralStringExpressionWithConstant();
  }
}
