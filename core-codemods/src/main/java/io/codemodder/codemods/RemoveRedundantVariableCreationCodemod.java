package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarIssuesPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod to remove redundant variable creation */
@Codemod(
    id = "sonar:java/remove-redundant-variable-creation-s1488",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveRedundantVariableCreationCodemod
    extends SonarIssuesPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public RemoveRedundantVariableCreationCodemod(
      @ProvidedSonarScan(ruleId = "java:S1488") final RuleIssue issues) {
    super(issues, ObjectCreationExpr.class);
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Issue sonarFinding) {

    // Get full block statement
    final Optional<BlockStmt> blockStmtOpt = objectCreationExpr.findAncestor(BlockStmt.class);
    if (blockStmtOpt.isPresent()) {
      final BlockStmt blockStmt = blockStmtOpt.get();

      // Retrieve return/throw statement to update its expression to given objectCreationExpr param
      final Optional<Statement> lastStmtOpt = blockStmt.getStatements().getLast();

      // Retrieve the redundant variable declaration expression that will be removed
      final Optional<ExpressionStmt> exprStmtOpt =
          objectCreationExpr.findAncestor(ExpressionStmt.class);

      if (lastStmtOpt.isPresent() && exprStmtOpt.isPresent()) {
        final Statement lastStmt = lastStmtOpt.get();
        if (lastStmt instanceof ReturnStmt returnStmt) {
          returnStmt.setExpression(objectCreationExpr);
        } else if (lastStmt instanceof ThrowStmt throwStmt) {
          throwStmt.setExpression(objectCreationExpr);
        } else {
          return ChangesResult.noChanges;
        }

        // Remove the redundant variable creation expression
        blockStmt.getStatements().remove(exprStmtOpt.get());

        return ChangesResult.changesApplied;
      }
    }

    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1488",
        "Local variables should not be declared and then immediately returned or thrown",
        "https://rules.sonarsource.com/java/RSPEC-1488");
  }
}
