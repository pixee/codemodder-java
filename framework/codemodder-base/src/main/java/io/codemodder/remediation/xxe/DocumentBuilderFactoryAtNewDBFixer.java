package io.codemodder.remediation.xxe;

import static io.codemodder.remediation.RemediationMessages.multipleCallsFound;
import static io.codemodder.remediation.RemediationMessages.noCallsAtThatLocation;
import static io.codemodder.remediation.xxe.XMLFeatures.addFeatureDisablingStatements;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import java.util.List;
import java.util.Optional;

/** Fix XXEs that are reported at the DocumentBuilderFactory.newDocumentBuilder() call locations. */
final class DocumentBuilderFactoryAtNewDBFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        ASTs.findMethodCallsWhichAreAssignedToType(
            cu, line, column, "newDocumentBuilder", List.of("DocumentBuilder"));

    if (candidateMethods.isEmpty()) {
      return new XXEFixAttempt(false, false, noCallsAtThatLocation);
    } else if (candidateMethods.size() > 1) {
      return new XXEFixAttempt(false, false, multipleCallsFound);
    }

    MethodCallExpr newDocumentBuilderCall = candidateMethods.get(0);

    // the scope must be the DocumentBuilderFactory
    Optional<Expression> newDocumentBuilderCallScope = newDocumentBuilderCall.getScope();
    if (newDocumentBuilderCallScope.isEmpty()) {
      return new XXEFixAttempt(true, false, "No scope found");
    }

    Expression scope = newDocumentBuilderCallScope.get();
    if (!scope.isNameExpr()) {
      return new XXEFixAttempt(true, false, "Scope is not a name");
    }

    Optional<Statement> statement = scope.findAncestor(Statement.class);
    if (statement.isEmpty()) {
      return new XXEFixAttempt(true, false, "No statement found");
    }
    return addFeatureDisablingStatements(scope.asNameExpr(), statement.get(), true);
  }
}
