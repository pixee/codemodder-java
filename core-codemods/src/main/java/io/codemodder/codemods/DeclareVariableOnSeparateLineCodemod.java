package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarIssuesPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for declaring a variable on a separate line. */
@Codemod(
    id = "sonar:java/declare-variable-on-separate-line-s1659",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DeclareVariableOnSeparateLineCodemod
    extends SonarIssuesPluginJavaParserChanger<VariableDeclarator> {
  @Inject
  public DeclareVariableOnSeparateLineCodemod(
      @ProvidedSonarScan(ruleId = "java:S1659") final RuleIssue issues) {
    super(issues, VariableDeclarator.class);
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Issue sonarFinding) {

    final Optional<Node> parentOptional = variableDeclarator.getParentNode();

    if (parentOptional.isEmpty()) {
      return ChangesResult.noChanges;
    }

    final NodeWithVariables<?> parentNode = (NodeWithVariables<?>) parentOptional.get();

    final DeclareVariableOnSeparateLine declareVariableOnSeparateLine;

    if (parentNode instanceof FieldDeclaration fieldDeclaration) {
      declareVariableOnSeparateLine =
          new DeclareVariableOnSeparateLineForFieldDeclaration(fieldDeclaration);
    } else {
      declareVariableOnSeparateLine =
          new DeclareVariableOnSeparateLineForVariableDeclarationExpr(
              (VariableDeclarationExpr) parentNode);
    }

    return declareVariableOnSeparateLine.splitVariablesIntoTheirOwnStatements()
        ? ChangesResult.changesApplied
        : ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1659",
        "Multiple variables should not be declared on the same line",
        "https://rules.sonarsource.com/java/RSPEC-1659/");
  }
}
