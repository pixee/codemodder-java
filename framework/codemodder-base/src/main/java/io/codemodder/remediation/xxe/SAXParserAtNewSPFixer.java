package io.codemodder.remediation.xxe;

import static io.codemodder.remediation.RemediationMessages.multipleNodesFound;
import static io.codemodder.remediation.RemediationMessages.noNodesAtThatLocation;
import static io.codemodder.remediation.xxe.XMLFeatures.addFeatureDisablingStatements;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import java.util.List;
import java.util.Optional;

/** Fix XXEs that are reported at the SAXParserFactory.newSAXParser() call locations. */
final class SAXParserAtNewSPFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        ASTs.findMethodCallsWhichAreAssignedToType(
            cu, line, column, "newSAXParser", List.of("SAXParser"));

    if (candidateMethods.isEmpty()) {
      return new XXEFixAttempt(false, false, noNodesAtThatLocation);
    } else if (candidateMethods.size() > 1) {
      return new XXEFixAttempt(false, false, multipleNodesFound);
    }

    MethodCallExpr newSaxParserCall = candidateMethods.get(0);

    // the scope must be the SAXParserFactory
    Optional<Expression> newSaxParserCallScope = newSaxParserCall.getScope();
    if (newSaxParserCallScope.isEmpty()) {
      return new XXEFixAttempt(true, false, "No scope found");
    }

    Expression scope = newSaxParserCallScope.get();
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
