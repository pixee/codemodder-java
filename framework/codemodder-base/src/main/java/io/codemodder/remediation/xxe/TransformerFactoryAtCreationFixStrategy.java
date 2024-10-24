package io.codemodder.remediation.xxe;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.ASTExpectations.expect;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

final class TransformerFactoryAtCreationFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }

    MethodCallExpr newFactoryInstanceCall = maybeCall.get();
    Optional<VariableDeclarator> newFactoryVariableRef =
        expect(newFactoryInstanceCall).toBeMethodCallExpression().initializingVariable().result();
    VariableDeclarator newFactoryVariable = newFactoryVariableRef.get();
    Optional<Statement> variableDeclarationStmtRef =
        newFactoryVariable.findAncestor(Statement.class);

    if (variableDeclarationStmtRef.isEmpty()) {
      return SuccessOrReason.reason("Not assigned as part of statement");
    }

    Statement statement = variableDeclarationStmtRef.get();
    Optional<BlockStmt> block = ASTs.findBlockStatementFrom(statement);
    if (block.isEmpty()) {
      return SuccessOrReason.reason("No block statement found for newFactory() call");
    }

    BlockStmt blockStmt = block.get();
    MethodCallExpr setAttributeCall =
        new MethodCallExpr(
            newFactoryVariable.getNameAsExpression(),
            "setAttribute",
            NodeList.nodeList(
                // add field access for javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD
                new FieldAccessExpr(new NameExpr("XMLConstants"), "ACCESS_EXTERNAL_DTD"),
                new StringLiteralExpr("")));

    addImportIfMissing(cu, "javax.xml.XMLConstants");
    Statement fixStatement = new ExpressionStmt(setAttributeCall);
    NodeList<Statement> existingStatements = blockStmt.getStatements();
    int index = existingStatements.indexOf(statement);
    existingStatements.add(index + 1, fixStatement);
    return SuccessOrReason.success();
  }

  /**
   * Matches against TransformerFactory.newInstance() calls
   *
   * @param node
   * @return
   */
  public static boolean match(final Node node) {
    return ASTs.isInitializedToType(node, "newInstance", List.of("TransformerFactory")).isPresent();
  }
}
