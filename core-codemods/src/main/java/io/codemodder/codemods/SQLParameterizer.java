package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.Either;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.ExpressionStmtVariableDeclaration;
import io.codemodder.ast.LocalScope;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.ast.TryResourceDeclaration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Contains most of the logic for detecting and fixing parameterizable SQL statements for a given
 * {@link MethodCallExpr}.
 */
public final class SQLParameterizer {

  private static final String preparedStatementNamePrefix = "stmt";
  private static final String preparedStatementNamePrefixAlternative = "statement";

  private final MethodCallExpr executeCall;

  private CompilationUnit compilationUnit;

  public SQLParameterizer(final MethodCallExpr methodCallExpr) {
    this.executeCall = Objects.requireNonNull(methodCallExpr);
    this.compilationUnit = null;
  }

  /**
   * Checks if the {@link MethodCallExpr} is of one of the execute calls of {@link
   * java.sql.Statement} whose argument is not a {@link String} literal.
   */
  public static boolean isParameterizationCandidate(final MethodCallExpr methodCallExpr) {
    // Maybe make this configurable? see:
    // https://github.com/find-sec-bugs/find-sec-bugs/wiki/Injection-detection
    try {
      final Predicate<MethodCallExpr> isExecute = SQLParameterizer::isSupportedJdbcMethodCall;

      final Predicate<MethodCallExpr> hasScopeSQLStatement =
          n ->
              n.getScope()
                  .filter(
                      s -> {
                        try {
                          String resolvedType = s.calculateResolvedType().describe();
                          return "java.sql.Statement".equals(resolvedType);
                        } catch (RuntimeException e) {
                          return false;
                        }
                      })
                  .isPresent();

      final Predicate<MethodCallExpr> isFirstArgumentNotSLE =
          n ->
              n.getArguments().getFirst().map(e -> !(e instanceof StringLiteralExpr)).orElse(false);
      // is execute of an statement object whose first argument is not a string?
      if (isExecute.and(hasScopeSQLStatement.and(isFirstArgumentNotSLE)).test(methodCallExpr)) {
        return true;
      }
      return false;

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (RuntimeException e) {
      return false;
    }
  }

  /** Returns true if this is a fixable JDBC method name. */
  public static boolean isSupportedJdbcMethodCall(final MethodCallExpr methodCall) {
    return fixableJdbcMethodNames.contains(methodCall.getNameAsString());
  }

  /** Returns a set of fixable JDBC method names. */
  public static Set<String> fixableJdbcMethodNames() {
    return fixableJdbcMethodNames;
  }

  private static final Set<String> fixableJdbcMethodNames =
      Set.of("executeQuery", "execute", "executeLargeUpdate", "executeUpdate");

  /**
   * Tries to find the source of an expression if it can be uniquely defined, otherwise, returns
   * self.
   */
  public static Expression resolveExpression(final Expression expr) {
    return Optional.of(expr)
        .map(e -> e instanceof NameExpr ? e.asNameExpr() : null)
        .flatMap(n -> ASTs.findEarliestLocalDeclarationOf(n.getName()))
        .map(s -> s instanceof LocalVariableDeclaration ? (LocalVariableDeclaration) s : null)
        // TODO currently it assumes it is never assigned, add support for definite assignments here
        .filter(ASTs::isFinalOrNeverAssigned)
        .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer())
        .map(SQLParameterizer::resolveExpression)
        .orElse(expr);
  }

  private Optional<MethodCallExpr> isConnectionCreateStatement(final Expression expr) {
    final Predicate<Expression> isConnection =
        e -> {
          try {
            return "java.sql.Connection".equals(e.calculateResolvedType().describe());
          } catch (RuntimeException ex) {
            return false;
          }
        };
    return Optional.of(expr)
        .map(e -> e instanceof MethodCallExpr ? expr.asMethodCallExpr() : null)
        .filter(
            mce ->
                mce.getScope().filter(isConnection).isPresent()
                    && mce.getNameAsString().equals("createStatement"));
  }

  private Optional<MethodCallExpr> validateExecuteCall(final MethodCallExpr executeCall) {
    // We first extract the largest method call possible containing executeCall
    var methodCall = executeCall;
    var maybeCall = Optional.of(methodCall);
    while (maybeCall.isPresent()) {
      maybeCall = maybeCall.flatMap(ASTs::isScopeInMethodCall);
      methodCall = maybeCall.orElse(methodCall);
    }
    // We require the call to be the first evaluated expression of its statement.
    // We're not sure how to do it yet, so we use a whitelist of common patterns
    // var rs = stmt.executeQuery()
    final Predicate<MethodCallExpr> isLocalInitExpr =
        call ->
            ASTs.isInitExpr(call)
                .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
                .isPresent();
    // rs = stmt.executeQuery
    final Predicate<MethodCallExpr> isAssigned = call -> ASTs.isAssigned(call).isPresent();
    // return stmt.executeQuery()
    final Predicate<MethodCallExpr> isReturned = call -> ASTs.isReturnExpr(call).isPresent();
    // stmt.executeQuery()
    final Predicate<MethodCallExpr> isCall =
        call -> call.getParentNode().filter(p -> p instanceof ExpressionStmt).isPresent();
    // TODO test this
    // try(ResultSet rs = conn.createStatement().executeQuery())
    final Predicate<MethodCallExpr> isFirstTryResource =
        call ->
            ASTs.isInitExpr(executeCall)
                .flatMap(ASTs::isResource)
                .flatMap(
                    pair ->
                        pair.getValue0()
                            .getResources()
                            .getFirst()
                            .filter(r -> r == pair.getValue1()))
                .isPresent();

    if (isLocalInitExpr
        .or(isAssigned)
        .or(isReturned)
        .or(isCall)
        .or(isFirstTryResource)
        .test(executeCall)) {
      return Optional.of(executeCall);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Finds if the Statement object was created by a <conn>.createStatement() call and returns either
   * the call itself, if immediate, or the {@link LocalVariableDeclaration} that has it as an
   * initializer.
   */
  private Optional<Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>>>
      findStatementCreationExpr(final MethodCallExpr executeCall) {
    // Has the form: <conn>.createStatement().executeQuery(...)
    // We require <conn> to be a nameExpr in this case.
    final Optional<Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>>>
        maybeImmediate =
            executeCall
                .getScope()
                .flatMap(this::isConnectionCreateStatement)
                // .filter(scope -> scope.getScope().filter(Expression::isNameExpr).isPresent())
                .map(Either::left);

    if (maybeImmediate.isPresent()) {
      return maybeImmediate;
    }

    // Has the form: <stmt>.executeQuery()
    // Find the <stmt> declaration
    final Optional<LocalVariableDeclaration> maybeLVD =
        executeCall
            .getScope()
            .map(expr -> expr instanceof NameExpr ? expr.asNameExpr() : null)
            .flatMap(ne -> ASTs.findEarliestLocalVariableDeclarationOf(ne, ne.getNameAsString()));

    // Has a single assignment
    // We erroniously assume that it always shadows the init expression
    // Needs some flow analysis to correctly address this case
    final Optional<AssignExpr> maybeSingleAssigned =
        maybeLVD
            .map(lvd -> ASTs.findAllAssignments(lvd).limit(2).toList())
            .filter(allAssignments -> allAssignments.size() == 1)
            .map(allAssignments -> allAssignments.get(0))
            .filter(assign -> assign.getTarget().isNameExpr())
            .filter(assign -> isConnectionCreateStatement(assign.getValue()).isPresent());

    if (maybeSingleAssigned.isPresent()) {
      return maybeSingleAssigned.map(a -> Either.right(Either.left(a)));
    }

    // Is <stmt> initialized with <conn>.createStatement()?
    final Optional<LocalVariableDeclaration> maybeInitExpr =
        maybeLVD.filter(
            lvd ->
                lvd.getVariableDeclarator()
                    .getInitializer()
                    .map(this::isConnectionCreateStatement)
                    .isPresent());

    return maybeInitExpr.map(init -> Either.right(Either.right(init)));
  }

  private Optional<Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>>>
      validateStatementCreationExpr(
          final Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>> stmtObject) {
    if (stmtObject.isRight()
        && stmtObject.getRight().isRight()
        && !canChangeTypes(stmtObject.getRight().getRight())) {
      return Optional.empty();
    }
    if (stmtObject.isRight()) {
      // For the assignment case, the declaration must be from an ExpressionStmt
      if (stmtObject.getRight().isLeft()) {
        final var maybelvd =
            ASTs.findEarliestLocalVariableDeclarationOf(
                    stmtObject.getRight().getLeft(),
                    stmtObject.getRight().getLeft().getTarget().asNameExpr().getNameAsString())
                .filter(lvd -> lvd instanceof ExpressionStmtVariableDeclaration);
        if (maybelvd.isEmpty()) {
          return Optional.empty();
        }

      } else {
        if (stmtObject.getRight().getRight() instanceof TryResourceDeclaration
            && !validateTryResource(
                (TryResourceDeclaration) stmtObject.getRight().getRight(), executeCall)) {
          return Optional.empty();
        }
      }
    }
    return Optional.of(stmtObject);
  }

  /** Checks if a local declaration can change types to a subtype. */
  private boolean canChangeTypes(final LocalVariableDeclaration localDeclaration) {
    final var allNameExpr =
        localDeclaration.getScope().stream()
            .flatMap(
                n ->
                    n
                        .findAll(
                            NameExpr.class,
                            ne -> ne.getNameAsString().equals(localDeclaration.getName()))
                        .stream());

    // if the only uses is being scope of a method calls, then we can change it
    // This is stronger than it needs to be
    return allNameExpr.allMatch(ne -> ASTs.isScopeInMethodCall(ne).isPresent());
  }

  /**
   * The Statement object must be able to change types and have the form <conn>.createStatement(),
   * where <conn> is an expression with Connection.
   */
  private boolean validateTryResource(
      final TryResourceDeclaration stmtObject, final MethodCallExpr executeCall) {
    // Essentially, we want the resource and call to be "next" to each other
    // (1) stmt resource is last and executeCall statement is the first on the try block
    final var maybeLastResource =
        stmtObject
            .getStatement()
            .getResources()
            .getLast()
            .filter(last -> last == stmtObject.getVariableDeclarationExpr());
    if (maybeLastResource.isPresent()
        && stmtObject
            .getStatement()
            .getTryBlock()
            .getStatements()
            .getFirst()
            .filter(
                first ->
                    ASTs.findParentStatementFrom(executeCall).filter(s -> s == first).isPresent())
            .isPresent()) {
      return true;
    }
    // (2) executeCall is an init expression of another resource next to stmtObject
    final var maybeInit =
        ASTs.isInitExpr(executeCall)
            .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
            .map(lvd -> lvd instanceof TryResourceDeclaration ? (TryResourceDeclaration) lvd : null)
            .filter(trd -> trd.getStatement() == stmtObject.getStatement());
    if (maybeInit.isPresent()) {
      final int stmtObjectIndex =
          stmtObject.getStatement().getResources().indexOf(stmtObject.getVariableDeclarationExpr());
      final int executeIndex =
          stmtObject
              .getStatement()
              .getResources()
              .indexOf(maybeInit.get().getVariableDeclarationExpr());
      return Math.abs(executeIndex - stmtObjectIndex) == 1;
    }
    return false;
  }

  private String generateNameWithSuffix(final String name, final Node start) {
    String actualName = preparedStatementNamePrefix;
    var maybeName = ASTs.findNonCallableSimpleNameSource(start, actualName);
    // Try for statement
    if (maybeName.isPresent()) {
      actualName = preparedStatementNamePrefixAlternative;
      maybeName = ASTs.findNonCallableSimpleNameSource(start, actualName);
      if (maybeName.isPresent()) {
        actualName = preparedStatementNamePrefix;
      }
    }
    int count = 0;
    String nameWithSuffix = actualName;
    while (maybeName.isPresent()) {
      count++;
      nameWithSuffix = actualName + count;
      maybeName = ASTs.findNonCallableSimpleNameSource(start, nameWithSuffix);
    }
    return count == 0 ? actualName : nameWithSuffix;
  }

  private List<Expression> fixInjections(
      final List<Deque<Expression>> injections, Map<Expression, Expression> resolvedMap) {
    final List<Expression> combinedExpressions = new ArrayList<>();
    for (final var injection : injections) {
      // fix start
      final var start = injection.removeFirst();
      final var startString = start.asStringLiteralExpr().getValue();
      final var builder = new StringBuilder(startString);
      final int lastQuoteIndex = startString.lastIndexOf('\'') + 1;
      final var prepend = startString.substring(lastQuoteIndex);
      builder.replace(lastQuoteIndex - 1, startString.length(), "?");
      start.asStringLiteralExpr().setValue(builder.toString());

      // fix end
      final var end = injection.removeLast();
      final var endString = end.asStringLiteralExpr().getValue();
      final int firstQuoteIndex = endString.indexOf('\'');
      final var newEnd = end.asStringLiteralExpr().getValue().substring(firstQuoteIndex + 1);
      final var append = endString.substring(0, firstQuoteIndex);
      end.asStringLiteralExpr().setValue(newEnd);

      // build expression for parameters
      var combined = buildParameter(injection, resolvedMap);
      // add the suffix of start
      if (prepend != "") {
        final var newCombined =
            new BinaryExpr(new StringLiteralExpr(prepend), combined, Operator.PLUS);
        combined = newCombined;
      }
      // add the prefix of end
      if (append != "") {
        final var newCombined =
            new BinaryExpr(combined, new StringLiteralExpr(append), Operator.PLUS);
        combined = newCombined;
      }
      combinedExpressions.add(combined);
    }
    return combinedExpressions;
  }

  private Expression unresolve(
      final Expression expr, final Map<Expression, Expression> resolutionMap) {
    Expression unresolved = expr;
    while (resolutionMap.get(unresolved) != null) {
      unresolved = resolutionMap.get(unresolved);
    }
    return unresolved;
  }

  private Expression buildParameter(
      final Deque<Expression> injectionExpressions, Map<Expression, Expression> resolutionMap) {
    final var it = injectionExpressions.iterator();
    Expression combined = it.next();
    boolean atLeastOneString = false;
    try {
      atLeastOneString = "java.lang.String".equals(combined.calculateResolvedType().describe());
    } catch (final Exception ignored) {
    }
    unresolve(combined, resolutionMap).replace(new StringLiteralExpr(""));

    while (it.hasNext()) {
      final var expr = it.next();
      try {
        if (!atLeastOneString
            && "java.lang.String".equals(expr.calculateResolvedType().describe())) {
          atLeastOneString = true;
        }
      } catch (final Exception ignored) {
      }
      unresolve(expr, resolutionMap).replace(new StringLiteralExpr(""));
      combined = new BinaryExpr(combined, expr, Operator.PLUS);
    }
    if (atLeastOneString) return combined;
    else return new BinaryExpr(combined, new StringLiteralExpr(""), Operator.PLUS);
  }

  /**
   * The fix consists of the following:
   *
   * <p>(0) If the execute call is the following resource, break the try into two statements;
   *
   * <p>(1.a) Create a new PreparedStatement pstmt object;
   *
   * <p>(1.b) Change Statement type to PreparedStatement and createStatement to prepareStatement;
   *
   * <p>(2) Add a setString for every injection parameter;
   *
   * <p>(3) Change <stmtCreation>.execute*() to pstmt.execute().
   */
  private MethodCallExpr fix(
      final Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>> stmtCreation,
      final QueryParameterizer queryParameterizer,
      final MethodCallExpr executeCall) {

    var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    // (0)
    if (stmtCreation.isRight()
        && stmtCreation.getRight().isRight()
        && executeStmt == stmtCreation.getRight().getRight().getStatement()) {
      final int stmtObjectIndex =
          stmtCreation
              .getRight()
              .getRight()
              .getStatement()
              .asTryStmt()
              .getResources()
              .indexOf(stmtCreation.getRight().getRight().getVariableDeclarationExpr());

      executeStmt =
          ASTTransforms.splitResources(
                  stmtCreation.getRight().getRight().getStatement().asTryStmt(), stmtObjectIndex)
              .getTryBlock()
              .getStatement(0);
    }

    final String stmtName =
        stmtCreation.ifLeftOrElseGet(
            mce -> generateNameWithSuffix(preparedStatementNamePrefix, mce),
            assignOrLVD ->
                assignOrLVD.ifLeftOrElseGet(
                    a -> a.getTarget().asNameExpr().getNameAsString(), lvd -> lvd.getName()));

    // (1)
    final var combinedExpressions =
        fixInjections(
            queryParameterizer.getInjections(),
            queryParameterizer.getLinearizedQuery().getResolvedExpressionsMap());

    var topStatement = executeStmt;
    for (int i = combinedExpressions.size() - 1; i >= 0; i--) {
      final var expr = combinedExpressions.get(i);
      ExpressionStmt setStmt = null;
      setStmt =
          new ExpressionStmt(
              new MethodCallExpr(
                  new NameExpr(stmtName),
                  "setString",
                  new NodeList<>(new IntegerLiteralExpr(String.valueOf(i + 1)), expr)));
      ASTTransforms.addStatementBeforeStatement(topStatement, setStmt);
      topStatement = setStmt;
    }

    ASTTransforms.addImportIfMissing(compilationUnit, "java.sql.PreparedStatement");

    // (2)
    final var args = new NodeList<Expression>();
    args.addFirst(queryParameterizer.getRoot());
    args.addAll(
        stmtCreation.ifLeftOrElseGet(
            mce -> mce.getArguments(),
            assignOrLVD ->
                assignOrLVD.ifLeftOrElseGet(
                    a -> a.getValue().asMethodCallExpr().getArguments(),
                    lvd ->
                        lvd.getVariableDeclarator()
                            .getInitializer()
                            .get()
                            .asMethodCallExpr()
                            .getArguments())));

    // (3)
    executeCall.setName("execute");
    executeCall.setScope(new NameExpr(stmtName));
    executeCall.setArguments(new NodeList<>());

    MethodCallExpr pstmtCreation;
    // (2.a)
    if (stmtCreation.isLeft()) {
      pstmtCreation =
          new MethodCallExpr(stmtCreation.getLeft().getScope().get(), "prepareStatement", args);
      final var pstmtCreationStmt =
          new ExpressionStmt(
              new VariableDeclarationExpr(
                  new VariableDeclarator(
                      StaticJavaParser.parseType("PreparedStatement"), stmtName, pstmtCreation)));
      ASTTransforms.addStatementBeforeStatement(topStatement, pstmtCreationStmt);

      // (2.b)
    } else {
      final var assignOrLVD = stmtCreation.getRight();
      if (assignOrLVD.isLeft()) {
        pstmtCreation = assignOrLVD.getLeft().getValue().asMethodCallExpr();
        pstmtCreation.setArguments(args);
        pstmtCreation.setName("prepareStatement");

        // change the assignment
        assignOrLVD.getLeft().setValue(StaticJavaParser.parseExpression("a"));
        assignOrLVD.getLeft().setValue(pstmtCreation);

        // change the initialization to be null and its type to PreparedStatement
        // This will only work assuming a single shadowing assignment, may require changes here in
        // the future
        var maybeLVD =
            ASTs.findEarliestLocalVariableDeclarationOf(
                assignOrLVD.getLeft().getTarget(),
                assignOrLVD.getLeft().getTarget().asNameExpr().getNameAsString());
        if (maybeLVD.isPresent()) {
          var vd = maybeLVD.get().getVariableDeclarator();
          vd.setInitializer(new NullLiteralExpr());
          vd.setType(StaticJavaParser.parseType("PreparedStatement"));
        }

      } else {
        assignOrLVD
            .getRight()
            .getVariableDeclarator()
            .setType(StaticJavaParser.parseType("PreparedStatement"));
        assignOrLVD
            .getRight()
            .getVariableDeclarator()
            .getInitializer()
            .ifPresent(expr -> expr.asMethodCallExpr().setName("prepareStatement"));
        assignOrLVD
            .getRight()
            .getVariableDeclarator()
            .getInitializer()
            .ifPresent(expr -> expr.asMethodCallExpr().setArguments(args));
        pstmtCreation =
            assignOrLVD
                .getRight()
                .getVariableDeclarator()
                .getInitializer()
                .get()
                .asMethodCallExpr();
      }
    }
    return pstmtCreation;
  }

  private boolean resolvedInScope(
      final Either<AssignExpr, LocalVariableDeclaration> assignOrLVD, Expression expr) {
    if (assignOrLVD.isLeft()) {
      final var scope =
          LocalScope.fromAssignExpression(
              assignOrLVD
                  .getLeft()); // Unsupported case for scope calculation, fail here until we add
      // some
      if (scope.stream().findAny().isEmpty()) {
        return true;
      }
      return scope.inScope(expr);
    }
    return assignOrLVD.getRight().getScope().inScope(expr);
  }

  private boolean assignedOrDefinedInScope(
      final NameExpr name, final Either<AssignExpr, LocalVariableDeclaration> assignOrLVD) {
    final var scope =
        assignOrLVD.ifLeftOrElseGet(a -> LocalScope.fromAssignExpression(a), lvd -> lvd.getScope());
    // Unsupported case for scope calculation, fail here until we add some
    if (scope.stream().findAny().isEmpty()) {
      return true;
    }
    final Stream<AssignExpr> assignmentsInScope =
        scope.stream()
            .flatMap(
                node -> node instanceof AssignExpr ? Stream.of((AssignExpr) node) : Stream.empty());

    final boolean assignedInScope =
        assignmentsInScope
            .flatMap(aexpr -> ASTs.hasNamedTarget(aexpr).stream())
            .anyMatch(nexpr -> nexpr.getNameAsString() == name.getNameAsString());

    final boolean definedInScope =
        ASTs.findNonCallableSimpleNameSource(name.getName())
            .filter(source -> scope.inScope(source))
            .isPresent();

    return assignedInScope || definedInScope;
  }

  /**
   * Checks if {@code methodCall} is a query call that needs to be fixed and fixes if that's the
   * case. If the parameterization happened, returns the PreparedStatement creation.
   */
  public Optional<MethodCallExpr> checkAndFix() {
    if (executeCall.findCompilationUnit().isPresent()) {
      this.compilationUnit = executeCall.findCompilationUnit().get();
    } else {
      return Optional.empty();
    }
    // validate the call itself first
    if (isParameterizationCandidate(executeCall) && validateExecuteCall(executeCall).isPresent()) {
      // Now find the stmt creation expression, if any and validate it
      final var stmtObject =
          findStatementCreationExpr(executeCall).flatMap(this::validateStatementCreationExpr);

      if (stmtObject.isPresent()) {
        // Now look for injections
        final QueryParameterizer queryp;
        // should not be emtpy
        if (executeCall.getArguments().isEmpty()) {
          return Optional.empty();
        }
        queryp = new QueryParameterizer(executeCall.getArgument(0));

        // Is any name resolved to an expression inside the scope of the Statement object?
        final boolean resolvedInScope =
            stmtObject
                .get()
                .ifLeftOrElseGet(
                    mcd -> false,
                    assignOrLVD ->
                        queryp.getLinearizedQuery().getResolvedExpressionsMap().keySet().stream()
                            .anyMatch(expr -> resolvedInScope(assignOrLVD, expr)));

        //// Is any name in the linearized expression defined/assigned inside the scope of the
        // Statement Object?
        final boolean nameInScope =
            stmtObject
                .get()
                .ifLeftOrElseGet(
                    mcd -> false,
                    assignOrLVD ->
                        queryp.getLinearizedQuery().getLinearized().stream()
                            .filter(expr -> expr.isNameExpr())
                            .map(expr -> expr.asNameExpr())
                            .anyMatch(name -> assignedOrDefinedInScope(name, assignOrLVD)));

        if (queryp.getInjections().isEmpty() || resolvedInScope || nameInScope) {
          return Optional.empty();
        }

        return Optional.of(fix(stmtObject.get(), queryp, executeCall));
      }
    }
    return Optional.empty();
  }
}
