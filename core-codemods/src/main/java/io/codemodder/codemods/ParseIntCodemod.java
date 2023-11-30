package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for automatically using the relevant integer parsing method . */
@Codemod(
    id = "sonar:java/parse-int-s2130",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class ParseIntCodemod extends CompositeJavaParserChanger {

  @Inject
  public ParseIntCodemod(
      final ParseIntCodemod.ParseMethodCallExprChanger parseMethodCallExprChanger) {
    super(parseMethodCallExprChanger);
  }

  private static class ParseMethodCallExprChanger
      extends SonarPluginJavaParserChanger<MethodCallExpr> {

    @Inject
    public ParseMethodCallExprChanger(
        @ProvidedSonarScan(ruleId = "java:S2130") final RuleIssues issues) {
      super(issues, MethodCallExpr.class, RegionNodeMatcher.MATCHES_START);
    }

    @Override
    public boolean onIssueFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr methodCallExpr,
        final Issue issue) {
      methodCallExpr.setName("parseInt");
      return true;
    }
  }
}
