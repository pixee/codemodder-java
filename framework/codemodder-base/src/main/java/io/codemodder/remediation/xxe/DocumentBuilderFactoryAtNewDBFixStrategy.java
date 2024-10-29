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
 * Fix strategy for XXE vulnerabilities anchored to the parser's newDocumentBuilder() calls. Finds
 * the parser's declaration and add statements disabling external entities and features.
 */
final class DocumentBuilderFactoryAtNewDBFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    var maybeCall =
        Optional.of(node).map(m -> m instanceof MethodCallExpr ? (MethodCallExpr) node : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }

    MethodCallExpr newDocumentBuilderCall = maybeCall.get();

    // the scope must be the DocumentBuilderFactory
    Optional<Expression> newDocumentBuilderCallScope = newDocumentBuilderCall.getScope();
    if (newDocumentBuilderCallScope.isEmpty()) {
      SuccessOrReason.reason("No scope found");
    }

    Expression scope = newDocumentBuilderCallScope.get();
    if (!scope.isNameExpr()) {
      return SuccessOrReason.reason("Scope is not a name");
    }

    Optional<Statement> statement = scope.findAncestor(Statement.class);
    if (statement.isEmpty()) {
      return SuccessOrReason.reason("No statement found");
    }
    return addFeatureDisablingStatements(scope.asNameExpr(), statement.get(), true);
  }

  /**
   * Matches DocumentBuilder y = (x.)newDocumentBuilder(), where the node is the newDocumentBuilder
   * call.
   *
   * @param node
   * @return
   */
  public static boolean match(final Node node) {
    return ASTs.isInitializedToType(node, "newDocumentBuilder", List.of("DocumentBuilder"))
        .isPresent();
  }
}
