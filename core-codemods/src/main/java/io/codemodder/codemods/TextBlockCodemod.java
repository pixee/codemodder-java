package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.*;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;
import java.util.Optional;

/** A codemod to replace string multiline concatenation with Text Block */
@Codemod(
    id = "sonar:java/text-block-s6126",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class TextBlockCodemod
    extends SonarPluginJavaParserChanger<Node> {

  @Inject
  public TextBlockCodemod(
      @ProvidedSonarScan(ruleId = "java:S6126") final RuleIssues issues) {
    super(issues, Node.class, RegionNodeMatcher.MATCHES_START);
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
