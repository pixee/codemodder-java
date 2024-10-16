package io.codemodder.remediation.xxe;

import static io.codemodder.javaparser.ASTExpectations.expect;
import static io.codemodder.remediation.RemediationMessages.multipleNodesFound;
import static io.codemodder.remediation.RemediationMessages.noNodesAtThatLocation;
import static io.codemodder.remediation.xxe.XMLFeatures.addFeatureDisablingStatements;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import java.util.List;
import java.util.Optional;

/**
 * Fix XXEs that are reported at the (DocumentBuilderFactory/SAXParserFactory).newInstance() call
 * locations.
 */
final class DocumentBuilderFactoryAndSAXParserAtCreationFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        ASTs.findMethodCallsWhichAreAssignedToType(
            cu, line, column, "newInstance", List.of("DocumentBuilderFactory", "SAXParserFactory"));

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
    return addFeatureDisablingStatements(
        newFactoryVariable.getNameAsExpression(), statement, false);
  }
}
