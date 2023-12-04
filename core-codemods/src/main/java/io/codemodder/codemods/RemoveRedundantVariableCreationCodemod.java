package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod to remove redundant variable creation */
@Codemod(
    id = "sonar:java/remove-redundant-variable-creation-s1488",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveRedundantVariableCreationCodemod
    extends SonarPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public RemoveRedundantVariableCreationCodemod(
      @ProvidedSonarScan(ruleId = "java:S1488") final RuleIssues issues) {
    super(issues, ObjectCreationExpr.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Issue issue) {

    // Get full block statement
    final Optional<BlockStmt> blockStmtOpt = objectCreationExpr.findAncestor(BlockStmt.class);
    if (blockStmtOpt.isPresent()) {
      final BlockStmt blockStmt = blockStmtOpt.get();

      // Retrieve return/throw statement to update its expression to given objectCreationExpr param
      final Optional<Statement> lastStmtOpt =
          blockStmt.getStatements().stream()
              .filter(stmt -> stmt instanceof ReturnStmt || stmt instanceof ThrowStmt)
              .findFirst();

      lastStmtOpt.ifPresent(
          lastStmt -> {
            final Expression expression = objectCreationExpr.clone();
            if (lastStmt instanceof ReturnStmt returnStmt) {
              returnStmt.setExpression(expression);
            }
            if (lastStmt instanceof ThrowStmt throwStmt) {
              throwStmt.setExpression(expression);
            }

            // Remove the redundant variable creation expression
            final Optional<ExpressionStmt> exprStmtOpt =
                objectCreationExpr.findAncestor(ExpressionStmt.class);
            exprStmtOpt.ifPresent(exprStmt -> blockStmt.getStatements().remove(exprStmt));
          });
    }

    return true;
  }
}
