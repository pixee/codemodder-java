package io.codemodder.codemods;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import java.util.Optional;
import java.util.function.Predicate;

public final class SQLParameterizer {

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

  private Optional<MethodCallExpr> isConnectionCreateStatement(Expression expr) {
    Predicate<Expression> isConnection =
        e -> e.calculateResolvedType().describe().equals("java.sql.Connection");
    return Optional.of(expr)
        .map(e -> e instanceof MethodCallExpr ? expr.asMethodCallExpr() : null)
        .filter(
            mce ->
                mce.getScope().filter(isConnection).isPresent()
                    && mce.getNameAsString().equals("createStatement"));
  }

  /**
   * Finds if the Statement object was created by a <conn>.createStatement() call and returns either
   * the call itself, if immediate, or the {@link LocalVariableDeclaration} that has it as an
   * initializer.
   */
  private Optional<Either<MethodCallExpr, LocalVariableDeclaration>> findStatementCreationExpr(
      MethodCallExpr executeCall) {
    // Is itself a MethodCallExpr like <conn>.createStatement
    Optional<Either<MethodCallExpr, LocalVariableDeclaration>> maybeImmediate =
        executeCall
            .getScope()
            .flatMap(scope -> isConnectionCreateStatement(scope))
            .map(mce -> Either.left(mce));
    // Is a NameExpr that is initialized with <conn>.createStatement
    Optional<Either<MethodCallExpr, LocalVariableDeclaration>> maybeLVD =
        executeCall
            .getScope()
            .map(expr -> expr instanceof NameExpr ? expr.asNameExpr() : null)
            .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()))
            .filter(
                lvd ->
                    lvd.getVariableDeclarator()
                        .getInitializer()
                        .map(init -> isConnectionCreateStatement(init))
                        .isPresent())
            .map(lvd -> Either.right(lvd));
    return maybeImmediate.or(() -> maybeLVD);
  }

  /** Checks if a local declaration can change types to a subtype. */
  private boolean canChangeTypes(LocalVariableDeclaration localDeclaration) {
    var allNameExpr =
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

  /** Returns true if the expression originates outside of method. */
  private boolean isTainted(Expression expr) {
    return false;
  }

  /**
   * The Statement object must be able to change types and have the form <conn>.createStatement(),
   * where <conn> is an expression with Connection.
   */
  private Optional<Either<MethodCallExpr, LocalVariableDeclaration>> validateStatementObject(
      MethodCallExpr executeCall) {
    var maybeExprOrVar = findStatementCreationExpr(executeCall);
    // If it is a immediate expression, all is good, otherwise, the local declaration must hold:
    // (1) Can change types to a subtype
    // If type is already PreparedStatement it's unlikely to be fixable (unlikely to appear,
    // unsupported)
    return maybeExprOrVar.filter(
        either ->
            either.isLeft()
                || either.isRight() && either.mapRight(lvd -> canChangeTypes(lvd)).getRight());
    // .filter(
    //    either ->
    //        either.isRight()
    //            && either.mapRight(lvd -> ASTs.isFinalOrNeverAssigned(lvd)).getRight());
  }

  private void fixStmtCreationExpr(MethodCallExpr stmtCreationExpr) {
    stmtCreationExpr.setName("prepareStatement");
    stmtCreationExpr.setArguments(new NodeList<>());
  }

  private void fixStatementDeclaration(LocalVariableDeclaration stmtDeclaration) {
    if (stmtDeclaration.getVariableDeclarationExpr().getVariables().size() == 1) {
      var vd = stmtDeclaration.getVariableDeclarator();
      vd.setType("PreparedStatement");
      ASTTransforms.addImportIfMissing(
          vd.findCompilationUnit().get(), "java.sql.PreparedStatement");
      vd.getInitializer().ifPresent(init -> fixStmtCreationExpr(init.asMethodCallExpr()));
    }
    // if multipledeclarations, split before
  }

  /**
   * The fix consists of the following:
   *
   * <p>(1) Change createStatement() to prepareStatement()
   *
   * <p>(1.b) Change type of Statement variable to PreparedStatement, if any
   *
   * <p>(2) Create a string argument for prepareStatement() with the query
   *
   * <p>(3) Create a setParameter() for every possible injection
   *
   * <p>(4) Change executeCall() to execute()
   */
  private void fix(
      Either<MethodCallExpr, LocalVariableDeclaration> stmtCreation, MethodCallExpr executeCall) {
    // (1) & (1.b)
    stmtCreation.ifPresentOrElse(this::fixStmtCreationExpr, this::fixStatementDeclaration);
    // (4)
    executeCall.setName("execute");
    executeCall.setArguments(new NodeList<>());
  }

  private void findInjections(Expression query) {}

  public boolean isVunerableCall(MethodCallExpr executeCall) {
    return false;
  }

  public void testAndFix(MethodCallExpr methodCallExpr) {
    if (isParameterizationCandidate(methodCallExpr)) {
      var stmtObject = validateStatementObject(methodCallExpr);
      stmtObject.ifPresent(e -> fix(e, methodCallExpr));
    }
  }
}
