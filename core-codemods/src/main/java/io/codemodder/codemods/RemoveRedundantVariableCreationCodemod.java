package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.SonarFinding;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod to remove redundant variable creation */
@Codemod(
    id = "sonar:java/remove-redundant-variable-creation-s1488",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveRedundantVariableCreationCodemod
    extends SonarPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public RemoveRedundantVariableCreationCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S1488")
          final RuleFinding issues) {
    super(issues, ObjectCreationExpr.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final SonarFinding sonarFinding) {

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
