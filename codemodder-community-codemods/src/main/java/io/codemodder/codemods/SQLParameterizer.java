package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
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
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

final class SQLParameterizer {

  public static final String lambdaName = "addAndReturnEmpty";
  public static final String parameterVectorName = "parameters";
  public static final String preparedStatementName = "preparedStatement";

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

      // java/sql/Statement.executeQuery(Ljava/lang/String;)Ljava/sql/ResultSet;:0 ...
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
    // For that, we look into a few common patterns
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
    if (isLocalInitExpr.or(isAssigned).or(isReturned).or(isCall).test(executeCall)) {
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
  private Optional<Either<MethodCallExpr, LocalVariableDeclaration>>
      findAndValidateStatementCreation(final MethodCallExpr executeCall) {
    final var maybeExprOrVar = findStatementCreationExpr(executeCall);
    // If it is an immediate expression, all is good, otherwise, the local declaration must hold:
    // (1) Can change types to a subtype
    // If type is already PreparedStatement it's unlikely to be fixable (unlikely to appear,
    // unsupported)
    return maybeExprOrVar.filter(
        either ->
            either.isLeft()
                || either.isRight() && either.mapRight(this::canChangeTypes).getRight());
    // .filter(
    //    either ->
    //        either.isRight()
    //            && either.mapRight(lvd -> ASTs.isFinalOrNeverAssigned(lvd)).getRight());
  }

  private void fixStmtCreationExpr(final MethodCallExpr stmtCreationExpr) {
    stmtCreationExpr.setName("prepareStatement");
    stmtCreationExpr.setArguments(new NodeList<>());
  }

  private void fixStatementDeclaration(final LocalVariableDeclaration stmtDeclaration) {
    if (stmtDeclaration.getVariableDeclarationExpr().getVariables().size() == 1) {
      final var vd = stmtDeclaration.getVariableDeclarator();
      vd.setType("PreparedStatement");
      ASTTransforms.addImportIfMissing(
          vd.findCompilationUnit().get(), "java.sql.PreparedStatement");
      // vd.getInitializer().ifPresent(init -> fixStmtCreationExpr(init.asMethodCallExpr()));
    }
    // TODO if multiple declarations, split before
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
    return "";
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

    final var cu = executeCall.findCompilationUnit().get();
    final var stmtName = preparedStatementName + generateId();

    // (1)
    final var methodBody =
        ASTs.findMethodBodyFrom(executeCall).flatMap(MethodDeclaration::getBody).get();
    ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), buildLambda());
    buildAndInitializeVector(injections.size())
        .forEach(s -> ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), s));

    ASTTransforms.addImportIfMissing(cu, "java.util.ArrayList");
    ASTTransforms.addImportIfMissing(cu, "java.util.BiFunction");
    ASTTransforms.addImportIfMissing(cu, "java.lang.StringBuilder");

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
    ASTTransforms.addImportIfMissing(cu, "java.sql.PreparedStatement");

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
   * <p>(2) Replace injectable expressions lambda call;
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
    final var cu = executeCall.findCompilationUnit().get();
    // TODO three cases (a) direct call, (b) indirect call, (c) indirect call with try declaration
    // TODO remove fors if there is a single expression injection

    final var stmtName = stmtCreation.getName();

    // (1)
    final var methodBody =
        ASTs.findMethodBodyFrom(executeCall).flatMap(MethodDeclaration::getBody).get();
    ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), buildLambda());
    buildAndInitializeVector(injections.size())
        .forEach(s -> ASTTransforms.addStatementBeforeStatement(methodBody.getStatement(0), s));

    ASTTransforms.addImportIfMissing(cu, "java.util.ArrayList");
    ASTTransforms.addImportIfMissing(cu, "java.util.BiFunction");
    ASTTransforms.addImportIfMissing(cu, "java.lang.StringBuilder");

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
    ASTTransforms.addImportIfMissing(cu, "java.sql.PreparedStatement");

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

  public boolean isVulnerableCall(final MethodCallExpr executeCall) {
    return false;
  }

  public boolean checkAndFix(final MethodCallExpr methodCallExpr) {
    // validate the call itself first
    if (isParameterizationCandidate(methodCallExpr)
        && validateExecuteCall(methodCallExpr).isPresent()) {
      // Now find the stmt creation expression, if any
      final var stmtObject = findStatementCreationExpr(methodCallExpr);
      // Now look for injections
      final var injections =
          new QueryParameterizer(methodCallExpr.getArgument(0)).checkAndGatherParameters();
      if (injections.isEmpty()) {
        return false;
      }
      // Finally apply fix
      stmtObject.ifPresent(
          e ->
              e.ifPresentOrElse(
                  call -> fix(call, injections, methodCallExpr.getArgument(0), methodCallExpr),
                  lvd -> fix(lvd, injections, methodCallExpr.getArgument(0), methodCallExpr)));
      return stmtObject.isPresent();
    }
    return false;
  }
}
