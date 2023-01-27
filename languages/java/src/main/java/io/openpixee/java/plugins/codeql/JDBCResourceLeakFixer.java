package io.openpixee.java.plugins.codeql;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import io.openpixee.java.ast.ASTPatterns;
import io.openpixee.java.ast.ASTs;
import java.util.Optional;
import java.util.function.Predicate;

public final class JDBCResourceLeakFixer {

  public static boolean isFixable(MethodCallExpr mce) {
    // Assumptions/Properties from CodeQL:
    // It is not a try resource, no close() is called within its scope (even if assigned).
    // There does not exists a "root" expression that is closed.
    // It does not escape its context: assigned to a field or returned.

    // We test for the following property. It has no descendent resource r that is:
    // (1) not closed, and
    // (2) escapes mce's scope.

    return false;
  }

  /** Fixes the leak of {@code mce} and returns its line. */
  public static int fix(MethodCallExpr mce) {
    int originalLine = mce.getRange().get().begin.line;
    // Actual fix here
    return originalLine;
  }

  /**
   * Checks if {@code expr} is part of a {@link MethodCallExpr} with close as its name (i.e {@code
   * <expr>.close()})
   */
  public static Optional<MethodCallExpr> isImmediatelyClosed(Expression expr) {
    return ASTPatterns.isScopeInMethodCall(expr)
        .filter(mce -> mce.getNameAsString().equals("close"));
  }

  /** Checks if a variable has {@code close()} called at some point, explicitly or implicitly. */
  public static boolean isClosed(VariableDeclarator vd) {
    // For now we don't have dataflow analysis to verify if close is called at every brach.
    // We instead check if close is called at any point. It matches CodeQL's behaviour
    var scope = ASTs.findLocalVariableScope(vd);
    Predicate<MethodCallExpr> isNameInMCEScope =
        mce ->
            mce.getScope()
                .filter(
                    m ->
                        m.isNameExpr()
                            && m.asNameExpr().getNameAsString().equals(vd.getNameAsString()))
                .isPresent();
    Predicate<MethodCallExpr> isCloseCall = mce -> mce.getNameAsString().equals("close");

    // explicit close() call at some point
    var closeCalled =
        scope.stream()
            .anyMatch(
                n ->
                    n.findFirst(
                            MethodCallExpr.class,
                            mce -> isNameInMCEScope.and(isCloseCall).test(mce))
                        .isPresent());
    if (closeCalled) return true;

    // implicit close() at some point
    // initialized as a try resource
    if (vd.getParentNode()
        .map(n -> n instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) n : null)
        .flatMap(vde -> ASTPatterns.isResource(vde))
        .isPresent()) return true;

    // or included in one as a NameExpr
    Predicate<TryStmt> isNameExprResource =
        stmt ->
            stmt.getResources().stream()
                .anyMatch(
                    expr ->
                        expr.isNameExpr()
                            && expr.asNameExpr().getNameAsString().equals(vd.getNameAsString()));

    if (scope.getStatements().stream()
        .anyMatch(n -> n.findFirst(TryStmt.class, isNameExprResource).isPresent())) return true;
    return false;
  }

  /**
   * Checks if an object created/acessed by {@code expr} has {@code close()} called at some point,
   * explicitly or implicitly.
   */
  public static boolean isClosed(Expression expr) {
    if (isImmediatelyClosed(expr).isPresent()) return true;

    // is named as a local variable v and v is closed
    // initialized
    if (ASTPatterns.isInitExpr(expr).filter(vd -> isClosed(vd)).isPresent()) return true;

    // assigned
    var maybeLocalVD =
        ASTPatterns.isAssigned(expr)
            .map(ae -> ae.getTarget() instanceof NameExpr ? (NameExpr) ae.getTarget() : null)
            .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()));
    if (maybeLocalVD.filter(triplet -> isClosed(triplet.getValue2())).isPresent()) return true;
    return false;
  }

  /**
   * Checks if an object created/acessed by {@code expr} escapes the scope of its encompassing
   * method immediately, that is, without begin assigned.
   */
  public static boolean immediatelyEscapesMethodScope(Expression expr) {
    // Returned or argument of a MethodCallExpr
    if (ASTPatterns.isReturnExpr(expr).isPresent()
        || ASTPatterns.isArgumentOfMethodCall(expr).isPresent()) return true;

    // Is assigned to a field?
    var maybeAE = ASTPatterns.isAssigned(expr);
    if (maybeAE.isPresent()) {
      var ae = maybeAE.get();
      // TODO Currently we have no precise way of knowing if the target is a field, thus
      // if the target is not a ExpressionName of a local variable, we consider it escaping
      Optional<NameExpr> maybeNameTarget =
          ae.getTarget().isNameExpr() ? Optional.of(ae.getTarget().asNameExpr()) : Optional.empty();
      var maybeLD =
          maybeNameTarget.flatMap(
              nameExpr ->
                  ASTs.findEarliestLocalDeclarationOf(nameExpr, nameExpr.getNameAsString()));
      if (maybeLD.isPresent()) return false;
      else return true;
    }
    return false;
  }

  /** Checks if a variable escapes the scope of its encompassing method. */
  public static boolean escapesMethodScope(VariableDeclarator vd) {
    var scope = ASTs.findLocalVariableScope(vd);

    // Returned
    Predicate<ReturnStmt> isReturned =
        rs ->
            rs.getExpression()
                .filter(
                    e ->
                        e.isNameExpr()
                            && e.asNameExpr().getNameAsString().equals(vd.getNameAsString()))
                .isPresent();
    if (scope.stream().anyMatch(n -> n.findFirst(ReturnStmt.class, isReturned).isPresent()))
      return true;

    // As an argument of a method call
    Predicate<MethodCallExpr> isCallArgument =
        mce ->
            mce.getArguments().stream()
                .anyMatch(
                    arg ->
                        arg.isNameExpr()
                            && arg.asNameExpr().getNameAsString().equals(vd.getNameAsString()));

    if (scope.stream().anyMatch(n -> n.findFirst(MethodCallExpr.class, isCallArgument).isPresent()))
      return true;

    // Assigned to a field
    // As before, we have no tools to precisely identify a field, so we detect any assignments whose
    // target is not a local variable
    Predicate<AssignExpr> isAssigned =
        ae ->
            ae.getValue().isNameExpr()
                && ae.getValue().asNameExpr().getNameAsString().equals(vd.getNameAsString());
    Predicate<AssignExpr> assignedToLocalVariable =
        ae ->
            ae.getTarget().isNameExpr()
                && ASTs.findEarliestLocalDeclarationOf(
                        ae.getTarget(), ae.getTarget().asNameExpr().getNameAsString())
                    .isPresent();

    if (scope.stream()
        .anyMatch(
            n ->
                n.findFirst(AssignExpr.class, isAssigned.and(assignedToLocalVariable.negate()))
                    .isPresent())) return true;
    return false;
  }

  /**
   * Checks if an object created/acessed by {@code expr} escapes the scope of its encompassing
   * method immediately.
   */
  public static boolean escapesMethodScope(Expression expr) {
    // immediately escapes or
    if (immediatelyEscapesMethodScope(expr)) return true;
    // is assigned/initialized to a variable and it escapes
    // initialized
    if (ASTPatterns.isInitExpr(expr).filter(vd -> escapesMethodScope(vd)).isPresent()) return true;

    // assigned
    var maybeLocalVD =
        ASTPatterns.isAssigned(expr)
            .map(ae -> ae.getTarget() instanceof NameExpr ? (NameExpr) ae.getTarget() : null)
            .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()));
    if (maybeLocalVD.filter(triplet -> escapesMethodScope(triplet.getValue2())).isPresent())
      return true;
    return false;
  }

  public static Optional<Integer> checkAndFix(MethodCallExpr mce) {
    if (isFixable(mce)) return Optional.of(fix(mce));
    return Optional.empty();
  }

  public static void main(String[] args) {
    String code =
        "class A {\n"
            + "private Connection conn;\n"
            + "private Statement stmt;\n"
            + "\n"
            + "  void outside(Statement stmt) {}\n"
            + "  void foo(String query) {\n"
            + "    Statement stmt;\n"
            + "    stmt = conn.createStatement();\n"
            + "    outside(stmt);\n"
            + "  }\n"
            + "}";
    final var combinedTypeSolver = new CombinedTypeSolver();
    StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    var cu = StaticJavaParser.parse(code);
    var mce = cu.findAll(MethodCallExpr.class).get(0);
    System.out.println(mce);
    System.out.println(escapesMethodScope(mce));
    LexicalPreservingPrinter.setup(cu);
    System.out.println(LexicalPreservingPrinter.print(cu));
  }
}
