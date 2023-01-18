package io.openpixee.java.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;
import java.util.function.Predicate;
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
        .filter(ts -> ts.getResources().stream().anyMatch(rs -> rs.equals(vde)));
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

  /**
   * Test for this pattern: {@link ForEachStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ForEachStmt, VariableDeclarationExpr, VariableDeclarator>>
      isForEachVariableDeclarationOf(final Node node, final String name) {
    final Predicate<VariableDeclarator> isVDOf = vd -> vd.getName().asString().equals(name);
    if (node instanceof ForEachStmt) {
      final ForEachStmt fstmt = (ForEachStmt) node;
      final var vde = fstmt.getVariable();
      final var maybeVD = vde.getVariables().stream().filter(isVDOf).findFirst();
      if (maybeVD.isPresent()) {
        return Optional.of(new Triplet<>(fstmt, vde, maybeVD.get()));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ForStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ForStmt, VariableDeclarationExpr, VariableDeclarator>>
      isForVariableDeclarationOf(final Node node, final String name) {
    final Predicate<VariableDeclarator> isVDOf = vd -> vd.getName().asString().equals(name);
    if (node instanceof ForStmt) {
      final ForStmt fstmt = (ForStmt) node;
      for (final var e : fstmt.getInitialization())
        if (e instanceof VariableDeclarationExpr) {
          final var maybeVD =
              e.asVariableDeclarationExpr().getVariables().stream().filter(isVDOf).findFirst();
          if (maybeVD.isPresent()) {
            return Optional.of(new Triplet<>(fstmt, e.asVariableDeclarationExpr(), maybeVD.get()));
          }
        }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link TryStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<TryStmt, VariableDeclarationExpr, VariableDeclarator>>
      isResourceOf(final Node node, final String name) {
    if (node instanceof TryStmt) {
      final var resources = ((TryStmt) node).getResources();
      for (final var e : resources) {
        final var maybePair = isVariableDeclarationExprOf(e, name);
        if (maybePair.isPresent()) {
          return Optional.of(
              new Triplet<>(
                  (TryStmt) node, maybePair.get().getValue0(), maybePair.get().getValue1()));
        }
      }
    }
    return Optional.empty();
  }
}
