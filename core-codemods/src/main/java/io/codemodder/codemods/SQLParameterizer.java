package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.Either;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.ast.TryResourceDeclaration;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.javatuples.Pair;

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
  static boolean isParameterizationCandidate(final MethodCallExpr methodCallExpr) {
    // Maybe make this configurable? see:
    // https://github.com/find-sec-bugs/find-sec-bugs/wiki/Injection-detection
    try {
      final Predicate<MethodCallExpr> isExecute =
          n ->
              (n.getNameAsString().equals("executeQuery")
                  || n.getNameAsString().equals("execute")
                  || n.getNameAsString().equals("executeLargeUpdate")
                  || n.getNameAsString().equals("executeUpdate"));

      // TODO will this catch PreparedStatement objects?
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

      final Predicate<MethodCallExpr> rule1 =
          isExecute.and(hasScopeSQLStatement.and(isFirstArgumentNotSLE));
      return rule1.test(methodCallExpr);

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (RuntimeException e) {
      return false;
    }
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
  private Optional<Either<MethodCallExpr, LocalVariableDeclaration>> findStatementCreationExpr(
      final MethodCallExpr executeCall) {
    // Is itself a MethodCallExpr like <conn>.createStatement
    // We require <conn> to be a nameExpr in this case.
    final Optional<Either<MethodCallExpr, LocalVariableDeclaration>> maybeImmediate =
        executeCall
            .getScope()
            .flatMap(this::isConnectionCreateStatement)
            // .filter(scope -> scope.getScope().filter(Expression::isNameExpr).isPresent())
            .map(Either::left);

    // Is a NameExpr that is initialized with <conn>.createStatement
    final Optional<Either<MethodCallExpr, LocalVariableDeclaration>> maybeLVD =
        executeCall
            .getScope()
            .map(expr -> expr instanceof NameExpr ? expr.asNameExpr() : null)
            .flatMap(ne -> ASTs.findEarliestLocalVariableDeclarationOf(ne, ne.getNameAsString()))
            .filter(
                lvd ->
                    lvd.getVariableDeclarator()
                        .getInitializer()
                        .map(this::isConnectionCreateStatement)
                        .isPresent())
            .map(Either::right);

    return maybeImmediate.or(() -> maybeLVD);
  }

  private Optional<Either<MethodCallExpr, LocalVariableDeclaration>> validateStatementCreationExpr(
      final Either<MethodCallExpr, LocalVariableDeclaration> stmtObject) {
    if (stmtObject.isRight() && !canChangeTypes(stmtObject.getRight())) {
      return Optional.empty();
    }
    if (stmtObject.isRight()
        && stmtObject.getRight() instanceof TryResourceDeclaration
        && !validateTryResource((TryResourceDeclaration) stmtObject.getRight(), executeCall)) {
      return Optional.empty();
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

  /** Removes an expression from an expression subtree. */
  private Expression collapse(final Expression e, final Expression root) {
    final var p = e.getParentNode().get();
    if (p instanceof BinaryExpr) {
      if (e.equals(((BinaryExpr) p).getLeft())) {
        final var child = ((BinaryExpr) p).getRight();
        if (p.equals(root)) {
          return child;
        } else {
          p.replace(child);
          return root;
        }
      }
      if (e.equals(((BinaryExpr) p).getRight())) {
        final var child = ((BinaryExpr) p).getLeft();
        if (p.equals(root)) {
          return child;
        } else {
          p.replace(child);
          return root;
        }
      }
    } else if (p instanceof EnclosedExpr) {
      return collapse((Expression) p, root);
    }
    e.remove();
    return root;
  }

  private Pair<List<Expression>, Expression> fixInjections(
      final List<Deque<Expression>> injections, Expression root) {
    final List<Expression> combinedExpressions = new ArrayList<>();
    for (final var injection : injections) {
      final var start = injection.removeFirst();
      final var startString = start.asStringLiteralExpr().getValue();
      final var builder = new StringBuilder(startString);
      builder.replace(startString.length() - 1, startString.length(), "?");
      start.asStringLiteralExpr().setValue(builder.toString());

      final var end = injection.removeLast();
      final var newEnd = end.asStringLiteralExpr().getValue().substring(1);
      if (newEnd.equals("")) {
        root = collapse(end, root);
      } else {
        end.asStringLiteralExpr().setValue(newEnd);
      }
      final var pair = combineExpressions(injection, root);
      combinedExpressions.add(pair.getValue0());
      root = pair.getValue1();
    }
    return new Pair<>(combinedExpressions, root);
  }

  private Pair<Expression, Expression> combineExpressions(
      final Deque<Expression> injectionExpressions, Expression root) {
    final var it = injectionExpressions.iterator();
    Expression combined = it.next();
    boolean atLeastOneString = false;
    try {
      atLeastOneString = "java.lang.String".equals(combined.calculateResolvedType().describe());
    } catch (final Exception ignored) {
    }
    root = collapse(combined, root);

    while (it.hasNext()) {
      final var expr = it.next();
      try {
        if (!atLeastOneString
            && "java.lang.String".equals(expr.calculateResolvedType().describe())) {
          atLeastOneString = true;
        }
      } catch (final Exception ignored) {
      }
      root = collapse(expr, root);
      combined = new BinaryExpr(combined, expr, Operator.PLUS);
    }
    if (atLeastOneString) return new Pair<>(combined, root);
    else
      return new Pair<>(new BinaryExpr(combined, new StringLiteralExpr(""), Operator.PLUS), root);
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
  private void fix(
      final Either<MethodCallExpr, LocalVariableDeclaration> stmtCreation,
      final QueryParameterizer queryParameterizer,
      final MethodCallExpr executeCall) {

    var newRoot = queryParameterizer.getRoot();
    var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    // (0)
    if (stmtCreation.isRight() && executeStmt == stmtCreation.getRight().getStatement()) {
      final int stmtObjectIndex =
          stmtCreation
              .getRight()
              .getStatement()
              .asTryStmt()
              .getResources()
              .indexOf(stmtCreation.getRight().getVariableDeclarationExpr());

      executeStmt =
          ASTTransforms.splitResources(
                  stmtCreation.getRight().getStatement().asTryStmt(), stmtObjectIndex)
              .getTryBlock()
              .getStatement(0);
    }

    final String stmtName =
        stmtCreation.ifLeftOrElseGet(
            mce -> generateNameWithSuffix(preparedStatementNamePrefix, mce), lvd -> lvd.getName());

    // (1)
    final var pair =
        fixInjections(queryParameterizer.getInjections(), queryParameterizer.getRoot());
    newRoot = pair.getValue1();
    final var combinedExpressions = pair.getValue0();

    var topStatement = executeStmt;
    for (int i = combinedExpressions.size() - 1; i >= 0; i--) {
      final var expr = combinedExpressions.get(i);
      final var setStmt =
          new ExpressionStmt(
              new MethodCallExpr(
                  new NameExpr(stmtName),
                  "setString",
                  new NodeList<>(new IntegerLiteralExpr(String.valueOf(i + 1)), expr)));
      ASTTransforms.addStatementBeforeStatement(topStatement, setStmt);
      topStatement = setStmt;
    }

    ASTTransforms.addImportIfMissing(compilationUnit, "java.sql.PreparedStatement");

    // Deleting some expressions may result in some String declarations with no initializer
    // We delete those.
    var allEmptyLVD =
        queryParameterizer.getStringDeclarations().stream()
            .filter(lvd -> lvd.getVariableDeclarator().getInitializer().isEmpty())
            .collect(Collectors.toSet());
    var newEmptyLVDs = allEmptyLVD;
    while (!newEmptyLVDs.isEmpty()) {
      for (var lvd : newEmptyLVDs) {
        for (final var ref : ASTs.findAllReferences(lvd)) {
          newRoot = collapse(ref, newRoot);
        }
        lvd.getVariableDeclarationExpr().removeForced();
      }
      newEmptyLVDs =
          queryParameterizer.getStringDeclarations().stream()
              .filter(lvd -> lvd.getVariableDeclarator().getInitializer().isEmpty())
              .collect(Collectors.toSet());
      newEmptyLVDs.removeAll(allEmptyLVD);
      allEmptyLVD.addAll(newEmptyLVDs);
    }

    // (2)
    final var args =
        stmtCreation.ifLeftOrElseGet(
            mce -> mce.getArguments(),
            lvd ->
                lvd.getVariableDeclarator()
                    .getInitializer()
                    .get()
                    .asMethodCallExpr()
                    .getArguments());
    args.addFirst(newRoot);

    // (2.a)
    if (stmtCreation.isLeft()) {
      final var pstmtCreationStmt =
          new ExpressionStmt(
              new VariableDeclarationExpr(
                  new VariableDeclarator(
                      StaticJavaParser.parseType("PreparedStatement"),
                      stmtName,
                      new MethodCallExpr(
                          stmtCreation.getLeft().getScope().get(), "prepareStatement", args))));
      ASTTransforms.addStatementBeforeStatement(topStatement, pstmtCreationStmt);

      // (2.b)
    } else {
      stmtCreation
          .getRight()
          .getVariableDeclarator()
          .setType(StaticJavaParser.parseType("PreparedStatement"));
      stmtCreation
          .getRight()
          .getVariableDeclarator()
          .getInitializer()
          .ifPresent(expr -> expr.asMethodCallExpr().setName("prepareStatement"));
      stmtCreation
          .getRight()
          .getVariableDeclarator()
          .getInitializer()
          .ifPresent(expr -> expr.asMethodCallExpr().setArguments(args));
    }

    // (3)
    executeCall.setName("execute");
    executeCall.setScope(new NameExpr(stmtName));
    executeCall.setArguments(new NodeList<>());
  }

  /**
   * Checks if {@code methodCall} is a query call that needs to be fixed and fixes if that's the
   * case.
   */
  public boolean checkAndFix() {

    if (executeCall.findCompilationUnit().isPresent()) {
      this.compilationUnit = executeCall.findCompilationUnit().get();
    } else {
      return false;
    }
    // validate the call itself first
    if (isParameterizationCandidate(executeCall) && validateExecuteCall(executeCall).isPresent()) {
      // Now find the stmt creation expression, if any and validate it
      final var stmtObject =
          findStatementCreationExpr(executeCall).flatMap(this::validateStatementCreationExpr);
      if (stmtObject.isPresent()) {
        // Now look for injections
        final var queryp = new QueryParameterizer(executeCall.getArgument(0));
        // If any of the strings used in the query is declared after the stmt object, reject
        final var queryInScope =
            stmtObject
                .get()
                .ifLeftOrElseGet(
                    mcd -> false,
                    stmtLVD ->
                        queryp.getStringDeclarations().stream()
                            .anyMatch(lvd -> stmtLVD.getScope().inScope(lvd.getStatement())));

        if (queryp.getInjections().isEmpty() || queryInScope) {
          return false;
        }
        fix(stmtObject.get(), queryp, executeCall);
        return true;
      }
    }
    return false;
  }
}
