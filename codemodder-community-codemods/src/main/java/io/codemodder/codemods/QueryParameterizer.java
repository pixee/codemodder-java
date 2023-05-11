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
    if (hasInjections(leaves) >= 1) {
      return gatherParameters(leaves);
    } else {
      return List.of();
    }
  }

  public Expression getRoot() {
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

  // TODO method calls/unary operators may have side effects and removing them may cause incorrect
  // TODO try(Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery()
  // TODO string variable with injection is no single use, abort?
  // behavior

  // Statement stmt = conn.createStatement();
  // int totalBefore = id;
  // String str = "user_" + (id++);
  // stmt.executeQuery("SELECT * FROM USERS WHERE id='" + id++ + "' AND name='" + str + "'");

  // solution: have the execute and creation be right next to each other

  // String str = "user_" + (id++);
  // int totalBefore = id;
  // Statement stmt = conn.createStatement();
  // stmt.executeQuery("SELECT * FROM USERS WHERE id='" + id++ + "' AND name='" + str + "'");

  // String str = "user_" + (id++);
  // String injection1Parameter = "user_" + (id++);
  // int totalBefore = id;
  // PreparedStatement stmt;
  // stmt = conn.prepareStatement("SELECT * FROM USERS WHERE id=? AND name='" + str + "'");
  // stmt.execute();

  // solution: add conn.prepareStatement only right before, and "cache" middle expressions
  // only delete "static" part of query.

  // Since expressions are evaluated left-to-right, a middle composed of multiple expressions will
  // always be a String type in the end
  // making PreparedStatement stmt; may result in problems (e.g. stmt.getConnection())

  // solution:
  // PreparedStatement stmt = conn.createStatement();
  // ...
  // String query = ...;
  // stmt.close();
  // stmt = conn.prepareStatement(query);
  // stmt.setString(0, parameter0);
  // <executeQueryStmt>

  // Allowed executeQuery stmts returnStmt, ExpressionStmt(AssignExpr,VariableDeclarationExprInit,
  // methodCallExpr), TryStmt -> VariableDeclarationExpr -> ...
  // maybe scope of single methodCallExpr in if?
  // Problems:
  // (1) How to maintain execution of injected expressions?

  // String query = "SELECT * FROM" + table + " WHERE name=' " + outside + "'";
  // String query = "SELECT * FROM USERS" + table + " WHERE name=?";
  // String parameter0 = outside + "";

  // Variable Access (NameExpr) are left alone
  // For every injection pattern: <head> + <before> + <middle> + <after> + <tail>
  // String str = <head> + <beforeafter>
  // String parameterX = ...
  // String tail = str + <tail>

  // solution:
  // foo(...){
  // List<String> parameters<ID> = new ArrayList<>(Collections.nCopies(<totalInjection#>,""));
  // ...
  // PreparedStatement stmt = conn.createStatement();
  // ...
  // String query<ID> = "..." + parameters.set(<combinedMiddle>, <injection#>) + "...";
  // stmt.close();
  // stmt = conn.prepareStatement(query);
  // for(int i<ID> =0; i<parameters<ID>.size(); i<ID>++) stmt.setString(parameters<ID>.get(i),i);
  // <executeQueryStmt>
  // ...}

  // problem: what if the middle begins in one string and ends in another?
  // String head = "...'" + "user_";
  // String tail = id + "'...";
  // stmt.executeQuery(head + tail);

  // List<List<String>> parameters<ID> = new ArrayList<>(Collections.nCopies)

  // problem: how to convert parameters.add(s) to ""

  // BiFunction<Integer,String,String> evaluateAndGather = s -> {parameters.set(parameters.get(i)
  // + s); return "";};

  // String str = "something";
  // String query = "...'" + out + str + "'...";
  // Expression combinedMiddle = null;
  // for (var expr : middle) {
  //  collapse(expr);
  //  if (combinedMiddle == null) {
  //    combinedMiddle = expr;
  //  } else {
  //    combinedMiddle = new BinaryExpr(combinedMiddle, expr, Operator.PLUS);
  //  }
  //  allString = allString && expr instanceof StringLiteralExpr;
  // }
  // if (!allString) {
  //  combinedMiddle = new BinaryExpr(combinedMiddle, new StringLiteralExpr(""), Operator.PLUS);
  // }
  // return Optional.of(combinedMiddle);

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
  int hasInjections(final Deque<Expression> query) {
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
    for (final var t : blacklist) if (type.describe().equals(t)) return false;
    return true;
  }

  private boolean isStartPattern(final Expression e) {
    return !(e instanceof StringLiteralExpr) || !e.asStringLiteralExpr().asString().endsWith("'");
  }

  private boolean isEndPattern(final Expression e) {
    return e instanceof StringLiteralExpr && e.asStringLiteralExpr().asString().startsWith("'");
  }
}
