package io.openpixee.java.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;
import org.javatuples.Pair;
import org.javatuples.Triplet;

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
  public static Optional<VariableDeclarator> isInitExpr(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof VariableDeclarator ? (VariableDeclarator) p : null);
  }

  /** Test for this pattern: {@link TryStmt} -&gt; {@link VariableDeclarationExpr} ({@code vde}) */
  public static Optional<TryStmt> isResource(final VariableDeclarationExpr vde) {
    return vde.getParentNode()
        .map(p -> p instanceof TryStmt ? (TryStmt) p : null)
        .filter(
            ts -> ts.getResources().stream().filter(rs -> rs.equals(vde)).findFirst().isPresent());
  }

  /**
   * Given an {@link Expression} {@code expr}, check if {@code expr} is the scope of a {@link
   * MethodCallExpr}.
   *
   * @return A {@link MethodCallExpr} with {@code expr} as its scope.
   */
  public static Optional<MethodCallExpr> isScopeInMethodCall(final Expression expr) {
    final var maybe = expr.getParentNode();
    return maybe
        .map(p -> p instanceof MethodCallExpr ? (MethodCallExpr) p : null)
        .filter(p -> (p.getScope().isPresent() && p.getScope().get().equals(expr)));
  }

  /**
   * Test for this pattern: {@link VariableDeclarationExpr} ({@code node}) -&gt; {@link
   * VariableDeclarator} -&gt; {@link SimpleName} ({@code name}).
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<VariableDeclarationExpr, VariableDeclarator>>
      isVariableDeclarationExprOf(final Expression node, final String name) {
    if (node instanceof VariableDeclarationExpr) {
      final VariableDeclarationExpr vde = node.asVariableDeclarationExpr();
      return vde.getVariables().stream()
          .filter(vd -> vd.getName().asString().equals(name))
          .findFirst()
          .map(vd -> new Pair<>(vde, vd));
    } else return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ExpressionStmt} ({@code node}) -&gt; {@link
   * VariableDeclarationExpr} -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code
   * name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ExpressionStmt, VariableDeclarationExpr, VariableDeclarator>>
      isExpressionStmtDeclarationOf(final Node node, final String name) {
    if (node instanceof ExpressionStmt) {
      final var exprStmt = (ExpressionStmt) node;
      final var maybePair = isVariableDeclarationExprOf(exprStmt.getExpression(), name);
      if (maybePair.isPresent()) {
        return Optional.of(
            new Triplet<>(
                (ExpressionStmt) node, maybePair.get().getValue0(), maybePair.get().getValue1()));
      }
    }
    return Optional.empty();
  }
}
