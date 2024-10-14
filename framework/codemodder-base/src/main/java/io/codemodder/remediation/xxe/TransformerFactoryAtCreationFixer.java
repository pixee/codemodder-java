package io.codemodder.remediation.xxe;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.ASTExpectations.expect;
import static io.codemodder.remediation.RemediationMessages.*;
import static io.codemodder.remediation.RemediationMessages.multipleNodesFound;

import com.github.javaparser.ast.CompilationUnit;
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
import java.util.List;
import java.util.Optional;

/** Fix XXEs reported at the TransformerFactory.newInstance() call locations. */
final class TransformerFactoryAtCreationFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        ASTs.findMethodCallsWhichAreAssignedToType(
            cu, line, column, "newInstance", List.of("TransformerFactory"));
    if (candidateMethods.isEmpty()) {
      return new XXEFixAttempt(false, false, noNodesAtThatLocation);
    } else if (candidateMethods.size() > 1) {
      return new XXEFixAttempt(false, false, multipleNodesFound);
    }
    MethodCallExpr newFactoryInstanceCall = candidateMethods.get(0);
    Optional<VariableDeclarator> newFactoryVariableRef =
        expect(newFactoryInstanceCall).toBeMethodCallExpression().initializingVariable().result();
    VariableDeclarator newFactoryVariable = newFactoryVariableRef.get();
    Optional<Statement> variableDeclarationStmtRef =
        newFactoryVariable.findAncestor(Statement.class);

    if (variableDeclarationStmtRef.isEmpty()) {
      return new XXEFixAttempt(true, false, "Not assigned as part of statement");
    }

    Statement statement = variableDeclarationStmtRef.get();
    Optional<BlockStmt> block = ASTs.findBlockStatementFrom(statement);
    if (block.isEmpty()) {
      return new XXEFixAttempt(true, false, "No block statement found for newFactory() call");
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
    return new XXEFixAttempt(true, true, null);
  }
}
