package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
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

      // Retrieve return statement to update its expression to objectCreationExpr param
      final Optional<ReturnStmt> returnStmtOpt =
          blockStmt.getStatements().stream()
              .filter(ReturnStmt.class::isInstance)
              .map(ReturnStmt.class::cast)
              .findFirst();

      returnStmtOpt.ifPresent(
          returnStmt -> {
            final Expression expression = objectCreationExpr.clone();
            returnStmt.setExpression(expression);

            // Remove the redundant variable creation expression
            final Optional<ExpressionStmt> exprStmtOpt =
                objectCreationExpr.findAncestor(ExpressionStmt.class);
            exprStmtOpt.ifPresent(exprStmt -> blockStmt.getStatements().remove(exprStmt));
          });
    }

    return true;
  }
}
