package io.openpixee.java.plugins.codeql;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
            .flatMap(
                n ->
                    n
                        .findFirst(
                            MethodCallExpr.class,
                            mce -> isNameInMCEScope.and(isCloseCall).test(mce))
                        .stream())
            .findAny()
            .isPresent();
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
                .filter(
                    expr ->
                        expr.isNameExpr()
                            && expr.asNameExpr().getNameAsString().equals(vd.getNameAsString()))
                .findFirst()
                .isPresent();
    if (scope.getStatements().stream()
        .flatMap(n -> n.findFirst(TryStmt.class, isNameExprResource).stream())
        .findFirst()
        .isPresent()) return true;
    return false;
  }

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

  public static Optional<Integer> checkAndFix(MethodCallExpr mce) {
    if (isFixable(mce)) return Optional.of(fix(mce));
    return Optional.empty();
  }

  public static void main(String[] args) {
    String code =
        "class A {\n"
            + "private Connection conn;\n"
            + "\n"
            + "  void foo(String query) {\n"
            + "    conn.createStatement().close();\n"
            + "  }\n"
            + "}";
    final var combinedTypeSolver = new CombinedTypeSolver();
    StaticJavaParser.getParserConfiguration()
        .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    var cu = StaticJavaParser.parse(code);
    var mce = cu.findAll(MethodCallExpr.class).get(1);
    System.out.println(mce);
    System.out.println(isClosed(mce));
    LexicalPreservingPrinter.setup(cu);
    System.out.println(LexicalPreservingPrinter.print(cu));
  }
}
