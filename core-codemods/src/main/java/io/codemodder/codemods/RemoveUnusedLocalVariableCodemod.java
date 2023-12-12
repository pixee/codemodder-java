package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;

/**   */
@Codemod(
    id = "sonar:java/remove-unused-local-variable-s1481",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUnusedLocalVariableCodemod extends SonarPluginJavaParserChanger<Node> {

  @Inject
  public RemoveUnusedLocalVariableCodemod(
      @ProvidedSonarScan(ruleId = "java:S1481") final RuleIssues issues) {
    super(issues, Node.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Node name,
      final Issue issue) {

    return true;
  }
}
