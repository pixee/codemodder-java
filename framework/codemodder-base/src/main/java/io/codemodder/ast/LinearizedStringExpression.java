package io.codemodder.ast;

import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Linearizes a string expression */
public final class LinearizedStringExpression {

  private final Map<Expression, Expression> resolvedExpressions;

  private final Expression root;

  private final Deque<Expression> linearized;

  public LinearizedStringExpression(final Expression expression) {
    this.resolvedExpressions = new IdentityHashMap<>();
    this.root = expression;
    this.linearized = linearize(expression).collect(Collectors.toCollection(ArrayDeque::new));
  }

  public Deque<Expression> getLinearized() {
    return this.linearized;
  }

  public Expression getRoot() {
    return this.root;
  }

  public Map<Expression, Expression> getResolvedExpressionsMap() {
    return this.resolvedExpressions;
  }

  /**
   * Linearizes a string expression. By linearization, it means that if a string is composed by the
   * concatenation of several string literals and expressions, it returns all the expressions in
   * order of concatenation.
   */
  private Stream<Expression> linearize(final Expression stringExpression) {
    return findLeaves(stringExpression);
  }

  /**
   * Finds the leaves of an expression tree whose internal nodes are BinaryExpr, EnclosedExpr and
   * NameExpr. Anything else is considered a leaf. Returns a Stream containing the leaves in
   * pre-order.
   */
  private Stream<Expression> findLeaves(final Expression e) {
    // EnclosedExpr and BinaryExpr are considered as internal nodes, so we recurse
    if (e instanceof EnclosedExpr) {
      if (calculateResolvedType(e)
          .filter(rt -> rt.describe().equals("java.lang.String"))
          .isPresent()) {
        return findLeaves(e.asEnclosedExpr().getInner());
      } else {
        return Stream.of(e);
      }
    }
    // Only BinaryExpr between strings should be considered
    else if (e instanceof BinaryExpr
        && e.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.PLUS)) {
      final var left = findLeaves(e.asBinaryExpr().getLeft());
      final var right = findLeaves(e.asBinaryExpr().getRight());
      return Stream.concat(left, right);
    }
    // NameExpr of String types should be recursively searched for more expressions.
    else if (e instanceof NameExpr
        && calculateResolvedType(e)
            .filter(rt -> rt.describe().equals("java.lang.String"))
            .isPresent()) {
      final var resolved = resolveLocalExpressionEmptyString(e);
      if (resolved != e) {
        return findLeaves(resolved);
      } else {
        return Stream.of(e);
      }
    }
    // Any other expression is a "leaf"
    return Stream.of(e);
  }

  private static Optional<ResolvedType> calculateResolvedType(final Expression e) {
    try {
      return Optional.of(e.calculateResolvedType());
    } catch (final RuntimeException exception) {
      return Optional.empty();
    }
  }

  /**
   * A special version of resolveExpression where initializer with empty strings are considered
   * empty. Also keeps track of the resolution.
   */
  private Expression resolveLocalExpressionEmptyString(final Expression expr) {
    // If this is a name, find its local declaration first
    var maybelvd =
        Optional.of(expr)
            .map(e -> e instanceof NameExpr ? e.asNameExpr() : null)
            .flatMap(n -> ASTs.findEarliestLocalDeclarationOf(n.getName()))
            .map(s -> s instanceof LocalVariableDeclaration ? (LocalVariableDeclaration) s : null);
    List<AssignExpr> first2Assignments =
        maybelvd.stream().flatMap(ASTs::findAllAssignments).limit(2).toList();

    Optional<Expression> maybeInit =
        maybelvd.flatMap(lvd -> lvd.getVariableDeclarator().getInitializer());
    // No assignments and an init
    if (maybeInit.isPresent() && first2Assignments.isEmpty()) {
      this.resolvedExpressions.put(maybeInit.get(), expr);
      return maybeInit.map(this::resolveLocalExpressionEmptyString).get();
    }

    // No init or empty string init but a single assignment?
    if (((maybeInit.isPresent()
                && maybeInit
                    .map(e -> e.isStringLiteralExpr() ? e.asStringLiteralExpr() : null)
                    .filter(sle -> Objects.equals(sle.asString(), ""))
                    .isPresent())
            || maybeInit.isEmpty())
        && first2Assignments.size() == 1) {
      this.resolvedExpressions.put(first2Assignments.get(0).getValue(), expr);
      return resolveLocalExpressionEmptyString(first2Assignments.get(0).getValue());
    }

    // failing that, return itself
    return expr;
  }
}
