package io.codemodder.remediation.missingsecureflag;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Adds flags based on being found at the "Cookie" instantiation in Jakarta API. */
final class FixAtJakartaCookieCreationStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var cookieCreationExpression =
        Optional.of(node).map(n -> n instanceof ObjectCreationExpr ? (ObjectCreationExpr) n : null);

    if (cookieCreationExpression.isPresent()) {
      // make sure this is part of a simple assignment statement, e.g., "Cookie cookie = new
      // Cookie(...)"
      var maybeStmt =
          ASTs.findParentStatementFrom(cookieCreationExpression.get())
              .filter(Statement::isExpressionStmt);

      if (maybeStmt.isEmpty()) {
        return SuccessOrReason.reason("Could not find expression statement containing call");
      }

      // get the variable declaration info so we can use it
      var maybeCookieName =
          cookieCreationExpression
              .get()
              .getParentNode()
              .map(vd -> vd instanceof VariableDeclarator ? (VariableDeclarator) vd : null)
              .map(NodeWithSimpleName::getNameAsExpression);

      if (maybeCookieName.isEmpty()) {
        return SuccessOrReason.reason("Could not find variable declaration expression");
      }

      final var newStatement =
          new ExpressionStmt(
              new MethodCallExpr(
                  maybeCookieName.get(),
                  "setSecure",
                  new NodeList<>(new BooleanLiteralExpr(true))));

      ASTTransforms.addStatementAfterStatement(maybeStmt.get(), newStatement);

      return SuccessOrReason.success();
    }
    return SuccessOrReason.reason("Not a method call with scope.");
  }
}
