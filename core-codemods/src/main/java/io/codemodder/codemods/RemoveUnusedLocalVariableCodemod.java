package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarIssuesPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Codemod to remove unused local variables which expression is a variable or just a Literal
 * expression like a single boolean, char, double, integer, long, null, string or a text block
 * string. We are not considering create object expression, method call expressions, condition
 * expressions, etc. because all of them have an expression node and that expression node could
 * result in a method call expression where a process could be performed and deleting it could
 * result on some unexpected behaviors.
 */
@Codemod(
    id = "sonar:java/remove-unused-local-variable-s1481",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUnusedLocalVariableCodemod
    extends SonarIssuesPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public RemoveUnusedLocalVariableCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S1481")
          final RuleFinding issues) {
    super(issues, VariableDeclarator.class);
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Issue sonarFinding) {

    final Optional<Expression> initializer = variableDeclarator.getInitializer();

    if (initializer.isPresent()) {
      final Expression initializerExpr = initializer.get();

      if (initializerExpr instanceof LiteralExpr || initializerExpr instanceof NameExpr) {
        final Optional<Node> variableDeclarationExprOptional = variableDeclarator.getParentNode();

        if (variableDeclarationExprOptional.isPresent()) {
          final VariableDeclarationExpr variableDeclarationExpr =
              (VariableDeclarationExpr) variableDeclarationExprOptional.get();

          if (1 == variableDeclarationExpr.getVariables().size()) {
            variableDeclarationExpr.removeForced();
          } else {
            final NodeList<VariableDeclarator> variables =
                variableDeclarationExpr.getVariables().stream()
                    .filter(variable -> !variable.equals(variableDeclarator))
                    .collect(Collectors.toCollection(NodeList::new));
            variableDeclarationExpr.setVariables(variables);
          }

          return ChangesResult.changesApplied;
        }
      }
    }

    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1481",
        "Unused local variables should be removed",
        "https://rules.sonarsource.com/java/RSPEC-1481");
  }
}
