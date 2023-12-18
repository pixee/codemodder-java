package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;

/** A codemod for declaring a variable on a separate line. */
@Codemod(
    id = "sonar:java/declare-variable-on-separate-line-s1659",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DeclareVariableOnSeparateLineCodemod extends SonarPluginJavaParserChanger<Node> {

  @Inject
  public DeclareVariableOnSeparateLineCodemod(
      @ProvidedSonarScan(ruleId = "java:S1659") final RuleIssues issues) {
    super(issues, Node.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Node node,
      final Issue issue) {

      return false;
  }
}
