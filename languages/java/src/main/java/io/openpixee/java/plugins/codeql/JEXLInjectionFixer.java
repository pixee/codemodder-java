package io.openpixee.java.plugins.codeql;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import java.util.Optional;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;

/**
 * A library that contains methods for automatically fixing JEXL injections detected by CodeQL's
 * rule "java/jexl-expression-injection" whenever possible.
 */
public final class JEXLInjectionFixer {

  /**
   * Detects if a {@link MethodCallExpr} evaluation of a {@link
   * JexlExpression#evaluate(org.apache.commons.jexl3.JexlContext)} can be sandboxed and tries to
   * fix it. Combines {@code isFixable} and {@code tryToFix}.
   */
  public static Optional<Integer> checkAndFix(final MethodCallExpr mce) {
    return isFixable(mce).flatMap(JEXLInjectionFixer::tryToFix);
  }

  /**
   * Returns true if there exists a local {@link JexlEngine} used to create and evaluate the
   * expression of {@code mce} that can be sandboxed.
   */
  public static Optional<MethodCallExpr> isFixable(final MethodCallExpr mce) {
    return findJEXLCreateExpression(mce).flatMap(JEXLInjectionFixer::findJEXLBuilderCreate);
  }

  /** Tries to sandbox the {@link JexlEngine#create()} and returns its line if it does. */
  public static Optional<Integer> tryToFix(final MethodCallExpr mce) {
    final var maybeStmt = ASTs.findParentStatementFrom(mce);
    if (maybeStmt.isEmpty()) {
      return Optional.empty();
    }
    final var sandboxType = StaticJavaParser.parseClassOrInterfaceType("JexlSandbox");

    // JexlSandbox sandbox = new JexlSandbox(true);
    final var sandboxDecl =
        new ExpressionStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(
                    sandboxType,
                    "sandbox",
                    new ObjectCreationExpr(
                        null, sandboxType, new NodeList<>(new BooleanLiteralExpr(true))))));

    // for(String cls : io.openpixee.security.UnwantedTypes.all())
    // 	sandbox.block(cls);
    final var sandboxFor =
        new ForEachStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(StaticJavaParser.parseType("String"), "cls")),
            new MethodCallExpr(new NameExpr("UnwantedTypes"), "all"),
            new ExpressionStmt(
                new MethodCallExpr(
                    new NameExpr("sandbox"), "block", new NodeList<>(new NameExpr("cls")))));

    final var stmt = maybeStmt.get();
    ASTTransforms.addStatementBeforeStatement(stmt, sandboxDecl);
    ASTTransforms.addStatementBeforeStatement(stmt, sandboxFor);

    final var sandboxCall =
        new MethodCallExpr(
            mce.getScope().get(), "sandbox", new NodeList<>(new NameExpr("sandbox")));
    final var newCreate = new MethodCallExpr(sandboxCall, "create");
    ASTTransforms.addImportIfMissing(
        mce.findCompilationUnit().get(), "io.openpixee.security.UnwantedTypes");
    ASTTransforms.addImportIfMissing(
        mce.findCompilationUnit().get(), "org.apache.commons.jexl3.introspection.JexlSandbox");
    // System.out.println(mce.findCompilationUnit().get().toString());
    mce.replace(newCreate);
    return mce.getBegin().map(b -> b.line);
  }

  /**
   * Given an {@code <expr>.evaluate()}, where {@code expr} is a {@link JexlExpression} object,
   * tries to find the {@link JexlEngine#createExpression(String)} method that spawns it.
   */
  private static Optional<MethodCallExpr> findJEXLCreateExpression(final MethodCallExpr mce) {
    // Always has a scope
    final var scope = mce.getScope().get();
    // immediate call
    if (scope instanceof MethodCallExpr) {
      if (scope.asMethodCallExpr().getName().equals("createExpression"))
        return Optional.of(scope.asMethodCallExpr());
    }

    // is a variable, track its definition
    // TODO should we care if it is final or never assigned?
    if (scope instanceof NameExpr) {
      final var maybeLVD =
          ASTs.findEarliestLocalDeclarationOf(
              scope.asNameExpr(), scope.asNameExpr().getNameAsString());
      return maybeLVD
          .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer())
          .map(expr -> expr.isMethodCallExpr() ? expr.asMethodCallExpr() : null)
          .filter(mcexpr -> mcexpr.getNameAsString().equals("createExpression"));
    }
    return Optional.empty();
  }

  // jexl.createExpression -> JexlBuilder.create()
  /**
   * Given an {@code <expr>.createExpression()}, where {@code expr} is a {@link JexlEngine} object,
   * tries to find the {@link JexlBuilder#create()} method that spawns it.
   */
  public static Optional<MethodCallExpr> findJEXLBuilderCreate(final MethodCallExpr mce) {
    // Always has a scope
    final var scope = mce.getScope().get();
    // immediate call
    if (scope instanceof MethodCallExpr) {
      if (scope.asMethodCallExpr().getName().equals("create"))
        return Optional.of(scope.asMethodCallExpr());
    }

    // is a variable, track its definition
    // TODO should we care if it is final or never assigned?
    if (scope instanceof NameExpr) {
      final var maybeLVD =
          ASTs.findEarliestLocalDeclarationOf(
              scope.asNameExpr(), scope.asNameExpr().getNameAsString());
      return maybeLVD
          .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer())
          .map(expr -> expr.isMethodCallExpr() ? expr.asMethodCallExpr() : null)
          .filter(mcexpr -> mcexpr.getNameAsString().equals("create"));
    }
    return Optional.empty();
  }
}
