package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
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
    extends SonarPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public RemoveUnusedLocalVariableCodemod(
      @ProvidedSonarScan(ruleId = "java:S1481") final RuleIssues issues) {
    super(issues, VariableDeclarator.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Issue issue) {

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

          return true;
        }
      }
    }

    return false;
  }
}
