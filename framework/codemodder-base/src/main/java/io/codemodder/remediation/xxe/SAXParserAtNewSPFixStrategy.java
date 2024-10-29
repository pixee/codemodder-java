package io.codemodder.remediation.xxe;

import static io.codemodder.remediation.xxe.XMLFixBuilder.addFeatureDisablingStatements;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

/**
 * Fix strategy for XXE vulnerabilities anchored to the SAXParser newSaxParser() calls. Finds the
 * parser's declaration and add statements disabling external entities and features.
 */
final class SAXParserAtNewSPFixStrategy implements RemediationStrategy {

  /**
   * Matches SaxParser y = (x.)newSaxParser(), where the node is the newSaxParser call.
   *
   * @param node
   * @return
   */
  public static boolean match(final Node node) {
    return ASTs.isInitializedToType(node, "newSAXParser", List.of("SAXParser")).isPresent();
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);

    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }

    MethodCallExpr newSaxParserCall = maybeCall.get();

    // the scope must be the SAXParserFactory
    Optional<Expression> newSaxParserCallScope = newSaxParserCall.getScope();
    if (newSaxParserCallScope.isEmpty()) {
      SuccessOrReason.reason("No scope found");
    }

    Expression scope = newSaxParserCallScope.get();
    if (!scope.isNameExpr()) {
      SuccessOrReason.reason("Scope is not a name");
    }

    Optional<Statement> statement = scope.findAncestor(Statement.class);
    if (statement.isEmpty()) {
      return SuccessOrReason.reason("No statement found");
    }
    return addFeatureDisablingStatements(scope.asNameExpr(), statement.get(), true);
  }
}
