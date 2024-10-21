package io.codemodder.remediation.missingsecureflag;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Default strategy to add missing secure flags in cookies. */
public final class MissingSecureFlagFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node)
            .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
            .filter(Node::hasScope);

    if (maybeCall.isPresent()) {
      var methodCallExpr = maybeCall.get();

      // This assumption is a bit strong, but covers the most common cases while avoiding weird
      // ones
      Optional<Statement> maybeStmt =
          methodCallExpr
              .getParentNode()
              .map(p -> p instanceof Statement ? (Statement) p : null)
              .filter(Statement::isExpressionStmt);
      if (maybeStmt.isEmpty()) {
        return SuccessOrReason.reason("Could not find expression statement containing call");
      }

      // We want to avoid things like: response.addCookie(new Cookie(...)), so we restrict
      // ourselves
      Optional<Expression> maybeCookieExpression =
          methodCallExpr.getArguments().stream()
              .findFirst()
              .filter(expr -> expr.isNameExpr() || expr.isFieldAccessExpr());

      if (maybeCookieExpression.isEmpty()) {
        return SuccessOrReason.reason("First argument is not a name or field access expression");
      }

      final var newStatement =
          new ExpressionStmt(
              new MethodCallExpr(
                  maybeCookieExpression.get(),
                  "setSecure",
                  new NodeList<>(new BooleanLiteralExpr(true))));

      ASTTransforms.addStatementBeforeStatement(maybeStmt.get(), newStatement);

      return SuccessOrReason.success();
    }
    return SuccessOrReason.reason("Not a method call with scope.");
  }
}
