package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.openpixee.security.ObjectInputFilters;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Adds gadget filtering logic to {@link java.io.ObjectInputStream}. */
@Codemod(
    id = "pixee:java/harden-java-deserialization",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenJavaDeserializationCodemod
    extends SemgrepJavaParserChanger<VariableDeclarationExpr> {

  @Inject
  public HardenJavaDeserializationCodemod(
      @SemgrepScan(ruleId = "harden-java-deserialization") final RuleSarif sarif) {
    super(sarif, VariableDeclarationExpr.class);
  }

  @Override
  public void onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarationExpr variableDeclarationExpr,
      final Result result) {
    Statement newStatement =
        generateFilterHardeningStatement(
            variableDeclarationExpr.getVariable(0).getNameAsExpression());

    // if we're not in an expression statement, we might be in a try-with-resources statement
    Optional<ExpressionStmt> wrappedExpression =
        variableDeclarationExpr.findAncestor(ExpressionStmt.class);
    if (wrappedExpression.isPresent()) {
      ExpressionStmt expressionStmt = wrappedExpression.get();
      ASTTransforms.addStatementAfterStatement(expressionStmt, newStatement);
      addImportIfMissing(cu, ObjectInputFilters.class.getName());
      return;
    }

    Optional<TryStmt> tryStmt = variableDeclarationExpr.findAncestor(TryStmt.class);
    if (tryStmt.isPresent()) {
      TryStmt tryStatement = tryStmt.get();
      BlockStmt tryBlock = tryStatement.getTryBlock();
      ASTTransforms.addStatementBeforeStatement(tryBlock.getStatements().get(0), newStatement);
      addImportIfMissing(cu, ObjectInputFilters.class.getName());
      return;
    }

    throw new IllegalArgumentException(
        "unexpected fix location: " + variableDeclarationExpr.getBegin().get());
  }

  /**
   * Generates an expression to invoke {@link
   * io.openpixee.security.ObjectInputFilters#enableObjectFilterIfUnprotected(ObjectInputStream)} on
   * the original scope (the {@link ObjectInputStream}).
   */
  private Statement generateFilterHardeningStatement(final Expression originalScope) {
    // this statement is the callback to our hardening code
    var callbackClass = new NameExpr(ObjectInputFilters.class.getSimpleName());
    var hardenStatement = new MethodCallExpr(callbackClass, "enableObjectFilterIfUnprotected");
    hardenStatement.addArgument(originalScope);
    return new ExpressionStmt(hardenStatement);
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
  }
}
