package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import java.util.Optional;
import javax.inject.Inject;

/** Fixes issues reported under the id "java/insecure-cookie". */
@Codemod(
    id = "codeql:java/insecure-cookie",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public class InsecureCookieCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public InsecureCookieCodemod(
      @ProvidedCodeQLScan(ruleId = "java/insecure-cookie") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    // This assumption is a bit strong, but covers the most common cases while avoiding weird ones
    final var maybeStmt =
        methodCallExpr
            .getParentNode()
            .map(p -> p instanceof Statement ? (Statement) p : null)
            .filter(Statement::isExpressionStmt);

    // We want to avoid things like: response.addCookie(new Cookie(...)), so we restrict ourselves
    final Optional<Expression> maybeCookieExpression =
        Optional.of(methodCallExpr.getArgument(0))
            .filter(expr -> expr.isNameExpr() || expr.isFieldAccessExpr());

    if (maybeStmt.isPresent() && maybeCookieExpression.isPresent()) {
      final var newStatement =
          new ExpressionStmt(
              new MethodCallExpr(
                  maybeCookieExpression.get(),
                  "setSecure",
                  new NodeList<>(new BooleanLiteralExpr(true))));

      ASTTransforms.addStatementBeforeStatement(maybeStmt.get(), newStatement);
      // Should we add a setSecure(false) after to retain behavior?
      return true;
    }
    return false;
  }
}
