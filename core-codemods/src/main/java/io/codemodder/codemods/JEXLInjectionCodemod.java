package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.github.pixee.security.UnwantedTypes;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;

/**
 * A codemod for automatically fixing JEXL injections detected by CodeQL's rule
 * "java/jexl-expression-injection" whenever possible.
 */
@Codemod(
    id = "codeql:java/jexl-expression-injection",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class JEXLInjectionCodemod extends SarifPluginJavaParserChanger<Expression> {

  @Inject
  public JEXLInjectionCodemod(
      @ProvidedCodeQLScan(ruleId = "java/jexl-expression-injection") final RuleSarif sarif) {
    super(sarif, Expression.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expression,
      final Result result) {
    return checkAndFix(expression).isPresent();
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }

  /**
   * Detects if a {@link Expression} that is the scope of a {@link
   * JexlExpression#evaluate(org.apache.commons.jexl3.JexlContext)} can be sandboxed and tries to
   * fix it. Combines {@code isFixable} and {@code tryToFix}.
   */
  static Optional<Integer> checkAndFix(final Expression expr) {
    return isFixable(expr).flatMap(JEXLInjectionCodemod::tryToFix);
  }

  /**
   * Checks if there exists a local {@link JexlBuilder#create()} call used to create and evaluate
   * the expression of {@code expr} that can be sandboxed.
   */
  static Optional<MethodCallExpr> isFixable(final Expression expr) {
    return findJEXLCreateExpression(expr).flatMap(JEXLInjectionCodemod::findJEXLBuilderCreate);
  }

  /** Tries to sandbox the {@link JexlBuilder#create()} and returns its line if it does. */
  static Optional<Integer> tryToFix(final MethodCallExpr mce) {
    final var cu = mce.findCompilationUnit().get();
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

    // for(String cls : io.github.pixee.security.UnwantedTypes.dangerousClassNameTokens())
    // 	sandbox.block(cls);
    final var sandboxFor =
        new ForEachStmt(
            new VariableDeclarationExpr(
                new VariableDeclarator(StaticJavaParser.parseType("String"), "cls")),
            new MethodCallExpr(
                new NameExpr(UnwantedTypes.class.getSimpleName()), "dangerousClassNameTokens"),
            new BlockStmt(
                new NodeList<>(
                    new ExpressionStmt(
                        new MethodCallExpr(
                            new NameExpr("sandbox"),
                            "block",
                            new NodeList<>(new NameExpr("cls")))))));

    final var stmt = maybeStmt.get();
    ASTTransforms.addStatementBeforeStatement(stmt, sandboxDecl);
    ASTTransforms.addStatementBeforeStatement(stmt, sandboxFor);

    // always has scope
    final var sandboxCall =
        new MethodCallExpr(
            mce.getScope().get(), "sandbox", new NodeList<>(new NameExpr("sandbox")));
    final var newCreate = new MethodCallExpr(sandboxCall, "create");
    ASTTransforms.addImportIfMissing(cu, UnwantedTypes.class.getName());
    ASTTransforms.addImportIfMissing(cu, "org.apache.commons.jexl3.introspection.JexlSandbox");
    mce.replace(newCreate);
    return mce.getBegin().map(b -> b.line);
  }

  /**
   * Given an expression {@code <expr>} that is the scope of an {@link
   * JexlExpression#evaluate(org.apache.commons.jexl3.JexlContext)} call, tries to find the {@link
   * JexlEngine#createExpression(String)} method that spawns it.
   */
  private static Optional<MethodCallExpr> findJEXLCreateExpression(final Expression expr) {
    // Is itself a createExpression
    if (expr instanceof MethodCallExpr) {
      if (expr.asMethodCallExpr().getNameAsString().equals("createExpression")) {
        return Optional.of(expr.asMethodCallExpr());
      }
    }

    // is a variable, track its definition
    if (expr instanceof NameExpr) {
      final var maybeLVD =
          ASTs.findEarliestLocalVariableDeclarationOf(
              expr.asNameExpr(), expr.asNameExpr().getNameAsString());
      return maybeLVD
          .filter(ASTs::isFinalOrNeverAssigned)
          .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer())
          .map(e -> e.isMethodCallExpr() ? e.asMethodCallExpr() : null)
          .filter(mcexpr -> mcexpr.getNameAsString().equals("createExpression"));
    }
    return Optional.empty();
  }

  /**
   * Given an {@code <expr>.createExpression()}, where {@code expr} is a {@link JexlEngine} object,
   * tries to find the {@link JexlBuilder#create()} method that spawns it.
   */
  private static Optional<MethodCallExpr> findJEXLBuilderCreate(final MethodCallExpr mce) {
    // Always has a scope
    final var scope = mce.getScope().get();
    // immediate call
    if (scope instanceof MethodCallExpr) {
      if (scope.asMethodCallExpr().getNameAsString().equals("create"))
        return Optional.of(scope.asMethodCallExpr());
    }

    // is a variable, track its definition
    if (scope instanceof NameExpr) {
      final var maybeLVD =
          ASTs.findEarliestLocalVariableDeclarationOf(
              scope.asNameExpr(), scope.asNameExpr().getNameAsString());
      return maybeLVD
          .filter(ASTs::isFinalOrNeverAssigned)
          .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer())
          .map(expr -> expr.isMethodCallExpr() ? expr.asMethodCallExpr() : null)
          .filter(mcexpr -> mcexpr.getNameAsString().equals("create"));
    }
    return Optional.empty();
  }
}
