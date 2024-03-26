package io.codemodder.codemods;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.ast.LinearizedStringExpression;
import io.codemodder.ast.LocalVariableDeclaration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;


final class QueryParameterizer {

  private final List<Deque<Expression>> injections;

  private final LinearizedStringExpression linearizedQuery;

  QueryParameterizer(final Expression query) {
    this.linearizedQuery = new LinearizedStringExpression(query);
    this.injections = gatherParameters(this.linearizedQuery.getLinearized());
  }

  Expression getRoot() {
    return linearizedQuery.getRoot();
  }

  List<Deque<Expression>> getInjections() {
    return injections;
  }

  LinearizedStringExpression getLinearizedQuery() {
    return linearizedQuery;
  }

  private Optional<ResolvedType> calculateResolvedType(final Expression e) {
    try {
      return Optional.of(e.calculateResolvedType());
    } catch (final RuntimeException exception) {
      return Optional.empty();
    }
  }

  /** Looks for the injection patterns and gather all expressions for each injection. */
  private List<Deque<Expression>> gatherParameters(final Deque<Expression> query) {
    final var parameters = new ArrayList<Deque<Expression>>();
    int modulo2 = 1;
    while (!query.isEmpty()) {
      var start = query.pop();
      while (!query.isEmpty() && !isStartPattern(start, modulo2)) {
        if (modulo2 == 0) {
          modulo2 = 1;
        }
        start = query.pop();
      }
      if (!query.isEmpty()) {
        final var middleExpressions = new ArrayDeque<Expression>();
        while (!query.isEmpty() && !isEndPattern(query.peekFirst())) {
          middleExpressions.add(query.pop());
        }

        // Is there any dangling quotes?
        if (query.isEmpty()) {
          return List.of();
        }
        var end = query.peekFirst();
        // end will be tested for start pattern, but we should not consider the first quote
        modulo2 = 0;

        // Either everything is a string literal or
        // there is a single expression that is not convertible to string
        if (!((middleExpressions.size() == 1 && !convertibleToString(middleExpressions.peek()))
            || middleExpressions.stream().allMatch(e -> e instanceof StringLiteralExpr))) {
          // add start and end
          middleExpressions.addFirst(start);
          middleExpressions.add(end);
          parameters.add(middleExpressions);
        }
      }
    }
    return parameters;
  }

  /** Checks if an expression is convertible to String in JDBC driver. */
  private boolean convertibleToString(final Expression exp) {
    // See
    // https://download.oracle.com/otn-pub/jcp/jdbc-4_2-mrel2-spec/jdbc4.2-fr-spec.pdf
    // for type conversions by setObject
    final var typeRef = calculateResolvedType(exp);
    if (typeRef.isEmpty()) {
      return true;
    }
    final var type = typeRef.get();

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

  private List<Integer> allIndexesOf(final char character, final String string) {
    List<Integer> indexes = new ArrayList<>();
    for (int currentIndex = string.indexOf(character);
        currentIndex >= 0;
        currentIndex = string.indexOf(character, currentIndex + 1)) {
      indexes.add(currentIndex);
    }
    return indexes;
  }

  private boolean isStartPattern(final Expression e, final int modulo2) {
    if (e != null && e.isStringLiteralExpr()) {
      var indexes = allIndexesOf('\'', e.asStringLiteralExpr().asString());
      return indexes.size() % 2 == modulo2;
    }
    return false;
  }

  private boolean isEndPattern(final Expression e) {
    if (e != null && e.isStringLiteralExpr()) {
      return e.asStringLiteralExpr().asString().indexOf('\'') >= 0;
    }
    return false;
  }
}
