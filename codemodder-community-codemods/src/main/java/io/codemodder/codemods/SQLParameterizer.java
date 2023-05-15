package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.ast.TryResourceDeclaration;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

final class SQLParameterizer {

  public static final String lambdaName = "addAndReturnEmpty";
  public static final String parameterVectorName = "parameters";
  public static final String preparedStatementName = "preparedStatement";

  final MethodCallExpr executeCall;

  CompilationUnit compilationUnit;

  BlockStmt methodBody;

  SQLParameterizer(final MethodCallExpr methodCallExpr) {
    this.executeCall = Objects.requireNonNull(methodCallExpr);
    this.compilationUnit = null;
    this.methodBody = null;
  }

  /**
   * Checks if the {@link MethodCallExpr} is of one of the execute calls of {@link
   * java.sql.Statement} whose argument is not a {@link String} literal.
   */
  public static boolean isParameterizationCandidate(final MethodCallExpr methodCallExpr) {
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
                  .filter(s -> s.calculateResolvedType().describe().equals("java.sql.Statement"))
                  .isPresent();

      final Predicate<MethodCallExpr> isFirstArgumentNotSLE =
          n ->
              n.getArguments().getFirst().map(e -> !(e instanceof StringLiteralExpr)).orElse(false);

      final Predicate<MethodCallExpr> rule1 =
          isExecute.and(hasScopeSQLStatement.and(isFirstArgumentNotSLE));
      return rule1.test(methodCallExpr);

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      return false;
    }
  }

  private Optional<MethodCallExpr> isConnectionCreateStatement(final Expression expr) {
    final Predicate<Expression> isConnection =
        e -> e.calculateResolvedType().describe().equals("java.sql.Connection");
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
            .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()))
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
      Either<MethodCallExpr, LocalVariableDeclaration> stmtObject) {
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
    // We first look if we can change the resource type to PreparedStatement
    if (!canChangeTypes(stmtObject)) {
      return false;
    }
    // Essentially, we want the resource and call to be "next" to each other
    // (1) stmt resource is last and executeCall statement is the first on the try block
    var maybeLastResource =
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
    var maybeInit =
        ASTs.isInitExpr(executeCall)
            .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
            .map(lvd -> lvd instanceof TryResourceDeclaration ? (TryResourceDeclaration) lvd : null)
            .filter(trd -> trd.getStatement() == stmtObject.getStatement());
    if (maybeInit.isPresent()) {
      int stmtObjectIndex =
          stmtObject.getStatement().getResources().indexOf(stmtObject.getVariableDeclarationExpr());
      int executeIndex =
          stmtObject
              .getStatement()
              .getResources()
              .indexOf(maybeInit.get().getVariableDeclarationExpr());
      return Math.abs(executeIndex - stmtObjectIndex) == 1;
    }
    return false;
  }

  private ExpressionStmt buildLambda() {
    final var stringParameter = "expression" + generateId();
    final var intParameter = "position" + generateId();
    final NodeList<Parameter> parameters = new NodeList<>();
    parameters.add(new Parameter(new UnknownType(), stringParameter));
    parameters.add(new Parameter(new UnknownType(), intParameter));
    final var body = new BlockStmt();
    final var lambda = new LambdaExpr(parameters, body);

    // parameter.get(position)
    final var parGet =
        new MethodCallExpr(
            new NameExpr(parameterVectorName), "get", new NodeList<>(new NameExpr(intParameter)));
    // parameters.get(position).append(s + "")
    final var concat =
        new MethodCallExpr(
            parGet,
            "append",
            new NodeList<>(
                new BinaryExpr(
                    new NameExpr(stringParameter),
                    new StringLiteralExpr(""),
                    BinaryExpr.Operator.PLUS)));
    body.addStatement(new ExpressionStmt(concat));

    // return "";
    body.addStatement(new ReturnStmt(new StringLiteralExpr("")));

    lambda.setBody(body);

    final var lambdaDecl =
        new VariableDeclarator(
            StaticJavaParser.parseType("BiFunction<Object,Integer,String>"),
            lambdaName + generateId(),
            lambda);
    return new ExpressionStmt(new VariableDeclarationExpr(lambdaDecl));
  }

  private String generateId() {
    return executeCall
        .getRange()
        .map(
            range ->
                "l"
                    + range.begin.line
                    + "c"
                    + range.begin.column
                    + "l"
                    + range.end.line
                    + "c"
                    + range.end.column)
        .orElse("");
  }

  private void fixInjections(final List<Deque<Expression>> injections) {
    int count = 0;
    for (final var injection : injections) {
      // TODO collapse here when size == 1
      final var start = injection.removeFirst();
      final var startString = start.asStringLiteralExpr().getValue();
      final var builder = new StringBuilder(startString);
      builder.replace(startString.length() - 1, startString.length(), "?");
      start.asStringLiteralExpr().setValue(builder.toString());

      final var end = injection.removeLast();
      final var newEnd = end.asStringLiteralExpr().getValue().substring(1);
      end.asStringLiteralExpr().setValue(newEnd);

      for (final var expr : injection) {
        final var newExpr = new MethodCallExpr();
        expr.replace(newExpr);
        newExpr.setName("apply");
        newExpr.setScope(new NameExpr(lambdaName + generateId()));
        newExpr.setArguments(new NodeList<>(expr, new IntegerLiteralExpr(String.valueOf(count))));
      }
      count++;
    }
  }

  private List<Statement> buildAndInitializeVector(final int nInjections) {
    final var parameterVectorType = StaticJavaParser.parseType("ArrayList<StringBuilder>");

    final var parameterInitialization =
        String.format("for(int %1$s=0; %1$s < %2$s; %1$s++)", "i" + generateId(), nInjections)
            + "{\n"
            + String.format("%1$s.add(new StringBuilder())", parameterVectorName + generateId())
            + ";\n}";
    final var parameterVectorCreation =
        new ObjectCreationExpr(
            null, parameterVectorType.asClassOrInterfaceType(), new NodeList<>());
    final var parameterDecl =
        new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    parameterVectorType,
                    parameterVectorName + generateId(),
                    parameterVectorCreation)));
    return List.of(StaticJavaParser.parseStatement(parameterInitialization), parameterDecl);
  }

  private Statement buildForSetParameters(final Expression scope) {
    // for(int i = 0; i<parameters.size(); i++){ stmt.setString(i, parameters.get(i+1).toString())}
    final var parameterSetStatement =
        String.format(
            "%3$s.setString(%1$s ,%2$s.get(%1$s + 1).toString())",
            "i" + generateId(), parameterVectorName + generateId(), scope);
    final var forParameters =
        String.format(
            "for(int %1$s=0; %1$s< %2$s.size(); %1$s++)",
            "i" + generateId(), parameterVectorName + generateId());
    return StaticJavaParser.parseStatement(forParameters + "{\n" + parameterSetStatement + ";\n}");
  }

  private void createLambdaAndParameterVector(final int vectorSize) {
    ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), buildLambda());
    buildAndInitializeVector(vectorSize)
        .forEach(s -> ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), s));

    ASTTransforms.addImportIfMissing(this.compilationUnit, "java.util.ArrayList");
    ASTTransforms.addImportIfMissing(this.compilationUnit, "java.util.BiFunction");
    ASTTransforms.addImportIfMissing(this.compilationUnit, "java.lang.StringBuilder");
  }

  /**
   * The fix consists of the following:
   *
   * <p>(1) Create the parameter vector and lambda at method start;
   *
   * <p>(2) Replace injectable expressions lambda call;
   *
   * <p>(3) Create a new PreparedStatement object right before call;
   *
   * <p>(4) Create a for including parameters in the parameter vector;
   *
   * <p>(5) Change executeCall() to execute().
   */
  private void fix(
      final MethodCallExpr stmtCreation,
      final List<Deque<Expression>> injections,
      final Expression root,
      final MethodCallExpr executeCall) {

    final var stmtName = preparedStatementName + generateId();

    // (1)
    createLambdaAndParameterVector(injections.size());

    // (2)
    fixInjections(injections);

    // (3)
    final var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    // stmt = stmt.getConnection().prepareStatement()
    final var arguments = new NodeList<>(root);
    stmtCreation.getArguments().stream()
        .flatMap(init -> init.asMethodCallExpr().getArguments().stream())
        .forEach(arguments::add);
    // prepareStatement()
    var prepareStatementCall =
        new MethodCallExpr(stmtCreation.getScope().get(), "prepareStatement", arguments);
    final var createPreparedStatement =
        new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    StaticJavaParser.parseType("PreparedStatement"),
                    stmtName,
                    prepareStatementCall)));
    ASTTransforms.addStatementBeforeStatement(executeStmt, createPreparedStatement);
    ASTTransforms.addImportIfMissing(this.compilationUnit, "java.sql.PreparedStatement");

    // (4)
    ASTTransforms.addStatementBeforeStatement(
        executeStmt, buildForSetParameters(new NameExpr(stmtName)));

    // (5)
    executeCall.setName("execute");
    executeCall.setScope(new NameExpr(stmtName));
    executeCall.setArguments(new NodeList<>());
  }

  /**
   * The fix consists of the following:
   *
   * <p>(1) Create the parameter vector and lambda at method start;
   *
   * <p>(2) Replace injectable expressions with lambda call;
   *
   * <p>(3) Change Statement type to PreparedStatement and createStatement() to prepareStatement();
   *
   * <p>(3.b) If the execute call is the following resource, break the try into two statements;
   *
   * <p>(4) Create a for including parameters in the parameter vector;
   *
   * <p>(5) Change executeCall() to execute().
   */
  private void fix(
      final TryResourceDeclaration stmtCreation,
      final List<Deque<Expression>> injections,
      final Expression root,
      final MethodCallExpr executeCall) {
    final var stmtName = stmtCreation.getName();
    // (1)
    createLambdaAndParameterVector(injections.size());

    // (2)
    fixInjections(injections);

    // (3)
    stmtCreation.getVariableDeclarator().setType(StaticJavaParser.parseType("PreparedStatement"));
    final var arguments = new NodeList<>(root);
    stmtCreation.getVariableDeclarator().getInitializer().stream()
        .flatMap(init -> init.asMethodCallExpr().getArguments().stream())
        .forEach(arguments::add);
    stmtCreation
        .getVariableDeclarator()
        .getInitializer()
        .ifPresent(expr -> expr.asMethodCallExpr().setName("prepareStatement"));
    stmtCreation
        .getVariableDeclarator()
        .getInitializer()
        .ifPresent(expr -> expr.asMethodCallExpr().setArguments(arguments));
    // (3.b)
    var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    if (executeStmt == stmtCreation.getStatement()) {
      int stmtObjectIndex =
          stmtCreation
              .getStatement()
              .getResources()
              .indexOf(stmtCreation.getVariableDeclarationExpr());

      executeStmt =
          ASTTransforms.splitResources(stmtCreation.getStatement(), stmtObjectIndex)
              .getTryBlock()
              .getStatement(0);
    }

    // (4)
    ASTTransforms.addStatementBeforeStatement(
        executeStmt, buildForSetParameters(new NameExpr(stmtName)));

    // (5)
    executeCall.setName("execute");
    executeCall.setScope(
        new EnclosedExpr(
            (new CastExpr(
                StaticJavaParser.parseType("PreparedStatement"), executeCall.getScope().get()))));
    executeCall.setArguments(new NodeList<>());
  }

  /**
   * The fix consists of the following:
   *
   * <p>(1) Create the parameter vector and lambda at method start;
   *
   * <p>(2) Replace injectable expressions with lambda call;
   *
   * <p>(3) create a new PreparedStatement right before call;
   *
   * <p>(4) Create a for including parameters in the parameter vector;
   *
   * <p>(5) Change executeCall() to execute().
   */
  private void fix(
      final LocalVariableDeclaration stmtCreation,
      final List<Deque<Expression>> injections,
      final Expression root,
      final MethodCallExpr executeCall) {
    // TODO remove fors if there is a single expression injection

    final var stmtName = stmtCreation.getName();

    // (1)
    createLambdaAndParameterVector(injections.size());

    // (2)
    fixInjections(injections);

    // (3)
    final var executeStmt = ASTs.findParentStatementFrom(executeCall).get();
    // stmt.close()
    ASTTransforms.addStatementBeforeStatement(
        executeStmt,
        new ExpressionStmt(new MethodCallExpr(new NameExpr(stmtCreation.getName()), "close")));

    // stmt = stmt.getConnection().prepareStatement()
    final var arguments = new NodeList<>(root);
    stmtCreation.getVariableDeclarator().getInitializer().stream()
        .flatMap(init -> init.asMethodCallExpr().getArguments().stream())
        .forEach(arguments::add);
    final var createPreparedStatement =
        new ExpressionStmt(
            new AssignExpr(
                new NameExpr(stmtName),
                new MethodCallExpr(
                    new MethodCallExpr(new NameExpr(stmtName), "getConnection"),
                    "prepareStatement",
                    arguments),
                AssignExpr.Operator.ASSIGN));
    ASTTransforms.addStatementBeforeStatement(executeStmt, createPreparedStatement);
    ASTTransforms.addImportIfMissing(this.compilationUnit, "java.sql.PreparedStatement");

    // (4)
    ASTTransforms.addStatementBeforeStatement(
        executeStmt,
        buildForSetParameters(
            new EnclosedExpr(
                new CastExpr(
                    StaticJavaParser.parseType("PreparedStatement"), new NameExpr(stmtName)))));

    // (5)
    executeCall.setName("execute");
    executeCall.setScope(
        new EnclosedExpr(
            (new CastExpr(
                StaticJavaParser.parseType("PreparedStatement"), executeCall.getScope().get()))));
    executeCall.setArguments(new NodeList<>());
  }

  public boolean checkAndFix() {

    var maybeMethodBody = ASTs.findMethodBodyFrom(executeCall).flatMap(MethodDeclaration::getBody);
    if (executeCall.findCompilationUnit().isPresent() && maybeMethodBody.isPresent()) {
      this.compilationUnit = executeCall.findCompilationUnit().get();
      this.methodBody = maybeMethodBody.get();
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
        final var injections =
            new QueryParameterizer(executeCall.getArgument(0)).checkAndGatherParameters();
        if (injections.isEmpty()) {
          return false;
        }
        if (stmtObject.get().isLeft()) {
          fix(stmtObject.get().getLeft(), injections, executeCall.getArgument(0), executeCall);
        } else {
          if (stmtObject.get().getRight() instanceof TryResourceDeclaration) {
            fix(
                (TryResourceDeclaration) stmtObject.get().getRight(),
                injections,
                executeCall.getArgument(0),
                executeCall);

          } else {
            fix(stmtObject.get().getRight(), injections, executeCall.getArgument(0), executeCall);
          }
        }
        return true;
      }
    }
    return false;
  }
}
