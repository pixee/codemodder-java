package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/optimize-jackson-string-usage",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class OptimizeJacksonStringUsageCodemod
    extends SarifPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public OptimizeJacksonStringUsageCodemod(
      @SemgrepScan(ruleId = "optimize-jackson-string-usage") RuleSarif semgrepSarif) {
    super(semgrepSarif, VariableDeclarator.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Result result) {
    ExpressionStmt mapperStmt = variableDeclarator.findAncestor(ExpressionStmt.class).get();
    Optional<Statement> statementBefore = ASTs.findStatementBefore(mapperStmt);
    if (statementBefore.isEmpty()) {
      return false;
    }
    ExpressionStmt jsonStmt = statementBefore.get().toExpressionStmt().get();
    MethodCallExpr mapperCall = variableDeclarator.getInitializer().get().asMethodCallExpr();
    VariableDeclarationExpr varDeclExpr = (VariableDeclarationExpr) jsonStmt.getChildNodes().get(0);
    Expression stream =
        varDeclExpr.getVariable(0).getInitializer().get().asMethodCallExpr().getArguments().get(0);
    jsonStmt.remove();
    mapperCall.getArguments().set(0, stream);
    return true;
  }
}
