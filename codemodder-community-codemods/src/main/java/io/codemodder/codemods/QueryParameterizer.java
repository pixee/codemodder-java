package io.codemodder.codemods;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.ast.ASTs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Checks and fixes injections in query expressions. */
final class QueryParameterizer {

  private final Expression root;

  QueryParameterizer(final Expression query) {
    this.root = query;
  }

  List<Deque<Expression>> checkAndGatherParameters() {
    final var leaves = findLeaves(root).collect(Collectors.toCollection(ArrayDeque::new));
    if (countInjections(leaves) >= 1) {
      return gatherParameters(leaves);
    } else {
      return List.of();
    }
  }

  Expression getRoot() {
    return root;
  }

  /**
   * Finds the leaves of an expression tree whose internal nodes are BinaryExpr, EnclosedExpr and
   * NameExpr. Anything else is considered a leaf. Returns a Stream containing the leaves in
   * pre-order.
   */
  private Stream<Expression> findLeaves(final Expression e) {
    // EnclosedExpr and BinaryExpr are considered as internal nodes, so we recurse
    if (e instanceof EnclosedExpr) {
      if (e.calculateResolvedType().describe().equals("java.lang.String"))
        return findLeaves(e.asEnclosedExpr().getInner());
      else {
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
        && e.calculateResolvedType().describe().equals("java.lang.String")) {
      // TODO consider fields and extract inits if any
      final var maybeSourceLVD =
          ASTs.findEarliestLocalDeclarationOf(e, e.asNameExpr().getNameAsString())
              .filter(ASTs::isFinalOrNeverAssigned)
              .filter(lvd -> ASTs.findAllReferences(lvd).size() == 1)
              .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer());
      return maybeSourceLVD.map(this::findLeaves).orElse(Stream.of(e));
    }
    // Any other expression is a "leaf"
    return Stream.of(e);
  }

  /** Looks for the injection patterns and gather all expressions for each injection. */
  private List<Deque<Expression>> gatherParameters(final Deque<Expression> query) {
    final var parameters = new ArrayList<Deque<Expression>>();
    while (!query.isEmpty()) {
      var start = query.pop();
      while (!query.isEmpty() && isStartPattern(start)) {
        start = query.pop();
      }
      if (!query.isEmpty()) {
        final var middleExpressions = new ArrayDeque<Expression>();
        while (!query.isEmpty() && !isEndPattern(query.peek())) {
          final var expr = query.pop();
          middleExpressions.add(expr);
        }
        // Either everything is a string literal or
        // there is a single expression that is not convertible to string
        if ((middleExpressions.size() == 1 && !convertibleToString(middleExpressions.peek()))
            || middleExpressions.stream().allMatch(e -> e instanceof StringLiteralExpr)) {
        } else {
          // add start and end
          middleExpressions.addFirst(start);
          middleExpressions.add(query.peek());
          parameters.add(middleExpressions);
        }
      }
    }
    return parameters;
  }

  /**
   * Checks if the deque containing the leaves of a query expression has any valid injection pattern
   * and no dangling quotes.
   */
  int countInjections(final Deque<Expression> query) {
    int count = 0;
    final var iterator = query.iterator();
    Expression start = null;
    while (iterator.hasNext()) {
      while (iterator.hasNext() && isStartPattern(start)) {
        start = iterator.next();
      }
      Expression end = null;
      while (iterator.hasNext() && !isEndPattern(end)) {
        end = iterator.next();
      }
      if (isEndPattern(end)) {
        count++;
        start = end;
      } else {
        // missing end quote, do nothing
        return 0;
      }
    }
    return count;
  }

  /** Checks if an expression is convertible to String in JDBC driver. */
  private boolean convertibleToString(final Expression exp) {
    // See
    // https://download.oracle.com/otn-pub/jcp/jdbc-4_2-mrel2-spec/jdbc4.2-fr-spec.pdf
    // for type conversions by setObject
    final var type = exp.calculateResolvedType();

    // primitive type?
    if (type.isPrimitive()) return false;
    // byte[] or Byte[]?
    if (type.isArray()
        && (type.asArrayType().getComponentType().describe().equals("byte")
            || type.asArrayType().getComponentType().describe().equals("java.lang.Byte")))
      return false;

    final var blacklist =
        List.of(
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.sql.Date",
            "java.sql.Time",
            "java.sql.Timestamp",
            "java.sql.Clob",
            "java.sql.Blob",
            "java.sql.Array",
            "java.sql.Struct",
            "java.sql.Ref",
            "java.sql.RowId",
            "java.sql.NClob",
            "java.sql.SQLXML",
            "java.util.Calendar",
            "java.util.Date",
            "java.time.LocalDate",
            "java.time.LocalTime",
            "java.time.LocalDateTime",
            "java.time.OffsetTime",
            "java.time.OffsetDateTime",
            "java.net.URL");
    for (final var t : blacklist) {
      if (type.describe().equals(t)) {
        return false;
      }
    }
    return true;
  }

  private boolean isStartPattern(final Expression e) {
    return !(e instanceof StringLiteralExpr) || !e.asStringLiteralExpr().asString().endsWith("'");
  }

  private boolean isEndPattern(final Expression e) {
    return e instanceof StringLiteralExpr && e.asStringLiteralExpr().asString().startsWith("'");
  }
}
