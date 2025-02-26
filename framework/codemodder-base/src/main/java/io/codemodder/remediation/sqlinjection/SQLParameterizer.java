package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
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

  public SQLParameterizer(final MethodCallExpr methodCallExpr, final CompilationUnit cu) {
    this.executeCall = Objects.requireNonNull(methodCallExpr);
    this.compilationUnit = cu;
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
      // is an `execute*()` call of a statement object whose first argument is not a string?
      return isExecute.and(hasScopeSQLStatement.and(isFirstArgumentNotSLE)).test(methodCallExpr);

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

  private Optional<MethodCallExpr> isConnectionCreateStatement(final Expression expr) {
    final Predicate<Expression> isConnection =
        e -> {
          try {
            return "java.sql.Connection".equals(e.calculateResolvedType().describe());
          } catch (RuntimeException ex) {
            return false;
          }
        };
    var stmtCreationMethods = List.of("createStatement", "prepareStatement");
    return Optional.of(expr)
        .map(e -> e instanceof MethodCallExpr ? expr.asMethodCallExpr() : null)
        .filter(
            mce ->
                mce.getScope().filter(isConnection).isPresent()
                    && (stmtCreationMethods.contains(mce.getNameAsString())));
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

    // Needs some flow analysis to correctly address this case
    final Optional<AssignExpr> maybeSingleAssigned =
        maybeLVD
            .map(lvd -> ASTs.findAllAssignments(lvd).limit(2).toList())
            .filter(allAssignments -> !allAssignments.isEmpty())
            .map(allAssignments -> allAssignments.get(allAssignments.size() - 1))
            .filter(assign -> assign.getTarget().isNameExpr())
            .filter(
                assign ->
                    isConnectionCreateStatement(ASTs.resolveLocalExpression(assign.getValue()))
                        .isPresent());

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

  private Optional<Either<AssignExpr, LocalVariableDeclaration>>
      validateStatementCreationExprForHijack(
          final Either<MethodCallExpr, Either<AssignExpr, LocalVariableDeclaration>> stmtObject) {
    if (stmtObject.isRight()) {
      var maybelvd =
          stmtObject
              .getRight()
              .ifLeftOrElseGet(
                  ae ->
                      ASTs.findEarliestLocalVariableDeclarationOf(
                          ae, ae.getTarget().asNameExpr().getNameAsString()),
                  lvd -> Optional.of(lvd));
      if (maybelvd.filter(lvd -> lvd instanceof ExpressionStmtVariableDeclaration).isPresent()) {
        return Optional.of(stmtObject.getRight());
      }
    }
    return Optional.empty();
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

  private String generateNameWithSuffix(final Node start) {
    String actualName = SQLParameterizer.preparedStatementNamePrefix;
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

  /**
   * Fix the injections by replacing the injected expressions with a `?` parameter.
   *
   * @param injections A list of deques representing the expressions.
   * @param resolvedMap A map containing the resolution of several expressions
   * @return The list of expressions that were being injected
   */
  private List<Expression> fixInjections(
      final List<Deque<Expression>> injections, final Map<Expression, Expression> resolvedMap) {
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
      if (!prepend.isEmpty()) {
        combined = new BinaryExpr(new StringLiteralExpr(prepend), combined, Operator.PLUS);
      }
      // add the prefix of end
      if (!append.isEmpty()) {
        combined = new BinaryExpr(combined, new StringLiteralExpr(append), Operator.PLUS);
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
   * Parameterize the query strings and add the `setParameter` calls.
   *
   * @param pStatementVariableName The name of the PreparedStatemetnVariable that is used as a scope
   *     for the `setParameter` calls.
   * @param anchoringStatement The statement that the `setParameter` calls will precede.
   * @param parameterizedQuery The parameterized query strings.
   * @return A statement that contains the start of
   */
  private Statement gatherAndSetParameters(
      final String pStatementVariableName,
      final Statement anchoringStatement,
      final QueryParameterizer parameterizedQuery) {
    // Parameterize the query strings
    final var queryParameters =
        fixInjections(
            parameterizedQuery.getInjections(),
            parameterizedQuery.getLinearizedQuery().getResolvedExpressionsMap());

    // Set the PreparedStatement parameters
    var topStatement = anchoringStatement;
    for (int i = queryParameters.size() - 1; i >= 0; i--) {
      final var expr = queryParameters.get(i);
      ExpressionStmt setStmt;
      setStmt =
          new ExpressionStmt(
              new MethodCallExpr(
                  new NameExpr(pStatementVariableName),
                  "setString",
                  new NodeList<>(new IntegerLiteralExpr(String.valueOf(i + 1)), expr)));
      ASTTransforms.addStatementBeforeStatement(topStatement, setStmt);
      topStatement = setStmt;
    }

    ASTTransforms.addImportIfMissing(compilationUnit, "java.sql.PreparedStatement");
    return topStatement;
  }

  /**
   * Apply the fix for the parameterization, which consists of the following steps:
   *
   * <p>(0) If the execute call is the following resource, break the try into two statements;
   *
   * <p>(1) Add a setString for every injection parameter;
   *
   * <p>(2.a) Create a new PreparedStatement pstmt object;
   *
   * <p>(2.b) Change Statement type to PreparedStatement and createStatement to prepareStatement;
   *
   * <p>(3) Change <stmtCreation>.execute*() to pstmt.execute().
   *
   * @param stmtCreation Either a declaration of a java.sql.Statement object, assingment of a
   *     java.sql.Statement object, or a conn.createStatement() call;
   * @param queryParameterizer The QueryParameterizer object that containing the query strings and
   *     parameter expressions
   * @param executeCall The `.execute*()` call.
   * @return
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
            mce -> generateNameWithSuffix(mce),
            assignOrLVD ->
                assignOrLVD.ifLeftOrElseGet(
                    a -> a.getTarget().asNameExpr().getNameAsString(),
                    LocalVariableDeclaration::getName));

    // (1)
    var topStatement = gatherAndSetParameters(stmtName, executeStmt, queryParameterizer);

    // (3)
    executeCall.setScope(new NameExpr(stmtName));
    executeCall.setArguments(new NodeList<>());

    // (2)
    // Gather execute call arguments
    final var args = new NodeList<Expression>();
    args.addFirst(queryParameterizer.getRoot());
    args.addAll(
        stmtCreation.ifLeftOrElseGet(
            MethodCallExpr::getArguments,
            assignOrLVD ->
                assignOrLVD.ifLeftOrElseGet(
                    a -> a.getValue().asMethodCallExpr().getArguments(),
                    lvd ->
                        lvd.getVariableDeclarator()
                            .getInitializer()
                            .get()
                            .asMethodCallExpr()
                            .getArguments())));

    // Create the `prepareStatement()` call and return it
    MethodCallExpr pstmtCreation;
    // Treat each of the three cases separately
    // (2.a) The statement is created directly from the Connection without a middle variable for the
    // java.sql.Statement
    if (stmtCreation.isLeft()) {
      // (2.b) The statement is created directly and assigned to a named variable
      pstmtCreation = createPSWithoutVariable(stmtCreation.getLeft(), args, topStatement, stmtName);
    } else {
      // The statement is created with an assignment or declaration
      final var assignOrLVD = stmtCreation.getRight();
      pstmtCreation =
          assignOrLVD.ifLeftOrElseGet(
              ae -> createPSFromAE(ae, args), lvd -> createPSFromLVD(lvd, args));
    }
    return pstmtCreation;
  }

  private MethodCallExpr createPSWithoutVariable(
      final MethodCallExpr directStatementCreation,
      final NodeList<Expression> args,
      final Statement anchoringStatement,
      final String stmtName) {
    var pstmtCreation =
        new MethodCallExpr(directStatementCreation.getScope().get(), "prepareStatement", args);
    final var pstmtCreationStmt =
        new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    StaticJavaParser.parseType("PreparedStatement"), stmtName, pstmtCreation)));
    ASTTransforms.addStatementBeforeStatement(anchoringStatement, pstmtCreationStmt);
    return pstmtCreation;
  }

  private MethodCallExpr createPSFromAE(
      final AssignExpr assignExpr, final NodeList<Expression> args) {
    var pstmtCreation = assignExpr.getValue().asMethodCallExpr();
    pstmtCreation.setArguments(args);
    pstmtCreation.setName("prepareStatement");

    // change the assignment
    assignExpr.setValue(StaticJavaParser.parseExpression("a"));
    assignExpr.setValue(pstmtCreation);

    // change the initialization to be null and its type to PreparedStatement
    // This will only work assuming a single shadowing assignment, may require changes here in
    // the future
    var maybeLVD =
        ASTs.findEarliestLocalVariableDeclarationOf(
            assignExpr.getTarget(), assignExpr.getTarget().asNameExpr().getNameAsString());
    if (maybeLVD.isPresent()) {
      var vd = maybeLVD.get().getVariableDeclarator();
      vd.setInitializer(new NullLiteralExpr());
      vd.setType(StaticJavaParser.parseType("PreparedStatement"));
    }
    return pstmtCreation;
  }

  private MethodCallExpr createPSFromLVD(
      final LocalVariableDeclaration localVariableDeclaration, final NodeList<Expression> args) {
    localVariableDeclaration
        .getVariableDeclarator()
        .setType(StaticJavaParser.parseType("PreparedStatement"));
    localVariableDeclaration
        .getVariableDeclarator()
        .getInitializer()
        .ifPresent(expr -> expr.asMethodCallExpr().setName("prepareStatement"));
    localVariableDeclaration
        .getVariableDeclarator()
        .getInitializer()
        .ifPresent(expr -> expr.asMethodCallExpr().setArguments(args));
    return localVariableDeclaration
        .getVariableDeclarator()
        .getInitializer()
        .get()
        .asMethodCallExpr();
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
            .anyMatch(nexpr -> Objects.equals(nexpr.getNameAsString(), name.getNameAsString()));

    final boolean definedInScope =
        ASTs.findNonCallableSimpleNameSource(name.getName()).filter(scope::inScope).isPresent();

    return assignedInScope || definedInScope;
  }

  private Expression getConnectionExpression(
      final Either<AssignExpr, LocalVariableDeclaration> stmtCreation) {
    return stmtCreation
        .ifLeftOrElseGet(
            ae -> ASTs.resolveLocalExpression(ae.getValue()).asMethodCallExpr(),
            lvd -> lvd.getDeclaration().getInitializer().get().asMethodCallExpr())
        .getScope()
        .get();
  }

  private MethodCallExpr fixByHijackedStatement(
      final Either<AssignExpr, LocalVariableDeclaration> stmtCreation,
      final QueryParameterizer queryParameterizer,
      final MethodCallExpr executeCall) {
    var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    // get the statement object variable name
    final String stmtName =
        stmtCreation.ifLeftOrElseGet(
            a -> a.getTarget().asNameExpr().getNameAsString(), LocalVariableDeclaration::getName);
    // generate a name for the new PreparedStatement object
    String pStmtName = generateNameWithSuffix(executeCall);

    final String connName = getConnectionExpression(stmtCreation).asNameExpr().getNameAsString();

    var topStatement = executeStmt;

    // Replace the parameters with the `?` string and adds the `setParameter` calls
    // Also, get the top `setParameter` statement
    topStatement = gatherAndSetParameters(pStmtName, topStatement, queryParameterizer);

    // Add PreparedStmt stmt =  conn.prepareStatement() assignment
    // Need to clone the nodes in the arguments to make sure the parent node is properly set
    MethodCallExpr prepareStatementCall =
        new MethodCallExpr(
            new NameExpr(connName),
            "prepareStatement",
            new NodeList<>(executeCall.getArguments().stream().map(n -> n.clone()).toList()));
    ExpressionStmt pStmtCreation =
        new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    StaticJavaParser.parseType("PreparedStatement"),
                    pStmtName,
                    prepareStatementCall)));
    ASTTransforms.addStatementBeforeStatement(topStatement, pStmtCreation);
    topStatement = pStmtCreation;
    ASTTransforms.addImportIfMissing(compilationUnit, "java.sql.PreparedStatement");

    // Test if stmt.execute*() is the first usage of the stmt object
    // If so, remove initializer
    // otherwise add stmt.close()
    if (isExecuteFirstUsageAfterDeclaration(stmtCreation, executeCall)) {
      var lvd = stmtCreation.getRight();
      lvd.getVariableDeclarator().getInitializer().ifPresent(i -> i.remove());
    } else {
      Statement closeOriginal =
          new ExpressionStmt(new MethodCallExpr(new NameExpr(stmtName), new SimpleName("close")));
      ASTTransforms.addStatementBeforeStatement(topStatement, closeOriginal);
    }

    // change execute statement
    executeCall.setScope(new NameExpr(pStmtName));
    executeCall.setArguments(new NodeList<>());

    // add stmt = pstmt after executeCall
    Statement hijackAssignment =
        new ExpressionStmt(
            new AssignExpr(
                new NameExpr(stmtName), new NameExpr(pStmtName), AssignExpr.Operator.ASSIGN));
    ASTTransforms.addStatementAfterStatement(executeStmt, hijackAssignment);

    return prepareStatementCall;
  }

  private boolean isExecuteFirstUsageAfterDeclaration(
      final Either<AssignExpr, LocalVariableDeclaration> stmtCreation,
      final MethodCallExpr executeCall) {
    if (stmtCreation.isRight()) {
      var lvd = stmtCreation.getRight();
      // This is heuristics
      return ASTs.findAllReferences(lvd).stream()
          .findFirst()
          .flatMap(e -> ASTs.isScopeInMethodCall(e))
          .filter(mce -> mce == executeCall)
          .isPresent();
    }
    // We could also apply this predicate to assignments and remove it, but that may require more
    // checks
    return false;
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
      final var stmtObject = findStatementCreationExpr(executeCall);

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

        ////// Is any name in the linearized expression defined/assigned inside the scope of the
        //// Statement Object?
        final boolean nameInScope =
            stmtObject
                .get()
                .ifLeftOrElseGet(
                    mcd -> false,
                    assignOrLVD ->
                        queryp.getLinearizedQuery().getLinearized().stream()
                            .filter(Expression::isNameExpr)
                            .map(Expression::asNameExpr)
                            .anyMatch(name -> assignedOrDefinedInScope(name, assignOrLVD)));

        // No injections detected
        if (queryp.getInjections().isEmpty()) {
          return Optional.empty();
        }

        // This means we can replace the Statement declaration or assignment
        if (!nameInScope
            && !resolvedInScope
            && stmtObject.flatMap(this::validateStatementCreationExpr).isPresent()) {
          return Optional.of(fix(stmtObject.get(), queryp, executeCall));
        }
        // Otherwise we use the hijack strategy
        var maybeStmtObject = stmtObject.flatMap(this::validateStatementCreationExprForHijack);
        if (maybeStmtObject.isPresent()) {
          return Optional.of(fixByHijackedStatement(maybeStmtObject.get(), queryp, executeCall));
        }
      }
    }
    return Optional.empty();
  }
}
