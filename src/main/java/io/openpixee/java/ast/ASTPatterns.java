package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;

/**
 * A static library for querying and returning patterns over AST nodes. Patterns are returned as
 * Optionals Tuples containing the pattern.
 */
public final class ASTPatterns {

  /**
   * Test for this pattern: {@link AssignExpr} -&gt; {@link Expression} ({@code expr}), where
   * ({@code expr}) is the right hand side expression of the assignment.
   */
  public static Optional<AssignExpr> isAssigned(Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof AssignExpr ? (AssignExpr) p : null)
        .filter(ae -> ae.getValue().equals(expr));
  }

  /**
   * Test for this pattern: {@link VariableDeclarationExpr} -&gt; {@link VariableDeclarator} -&gt;
   * {@link Expression} ({@code expr})
   */
  public static Optional<VariableDeclarationExpr> isInitExpr(Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof VariableDeclarator ? (VariableDeclarator) p : null)
        .flatMap(p -> p.getParentNode())
        .map(gp -> gp instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) gp : null);
  }

  /** Test for this pattern: {@link TryStmt} -&gt; {@link VariableDeclarationExpr} ({@code vde}) */
  public static Optional<TryStmt> isResource(VariableDeclarationExpr vde) {
    return vde.getParentNode()
        .map(p -> p instanceof TryStmt ? (TryStmt) p : null)
        .filter(
            ts -> ts.getResources().stream().filter(rs -> rs.equals(vde)).findFirst().isPresent());
  }
}
