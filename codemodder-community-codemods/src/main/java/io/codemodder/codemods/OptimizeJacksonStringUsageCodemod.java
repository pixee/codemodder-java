package io.codemodder.codemods;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/optimize-jackson-string-usage",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class OptimizeJacksonStringUsageCodemod
    extends SarifPluginJavaParserChanger<ExpressionStmt> {

  @Inject
  public OptimizeJacksonStringUsageCodemod(
      @SemgrepScan(ruleId = "optimize-jackson-string-usage") RuleSarif semgrepSarif) {
    super(semgrepSarif, ExpressionStmt.class, RegionExtractor.FROM_FIRST_THREADFLOW_EVENT);
  }

  /**
   * The rule pattern identifies any dataflow from IOUtils#toString() to ObjectMapper#readValue().
   * However, we can't hope to handle every instance of that pattern. This codemod will operate when
   * the given requirements hold:
   *
   * <ol>
   *   <li>The IOUtils#toString() call is a simple variable declaration assignment.
   *   <li>The resulting String is used nowhere else besides the readValue() call.
   * </ol>
   *
   * We've configured the {@link RegionExtractor} to pull the first data flow event, which is
   */
  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ExpressionStmt varDeclStmt,
      final Result result) {

    VariableDeclarationExpr varDeclExpr = varDeclStmt.getExpression().asVariableDeclarationExpr();
    String variableName = varDeclExpr.getVariable(0).getNameAsString();
    Expression stream =
        varDeclExpr.getVariable(0).getInitializer().get().asMethodCallExpr().getArgument(0);
    List<ThreadFlowLocation> threadFlow =
        result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations();
    Region lastEventLocation =
        threadFlow.get(threadFlow.size() - 1).getLocation().getPhysicalLocation().getRegion();
    Optional<MethodDeclaration> methodDeclaration =
        varDeclStmt.findAncestor(MethodDeclaration.class);
    if (methodDeclaration.isEmpty()) {
      return false;
    }

    /*
     * This quite is check robust -- it makes sure that at the last SARIF event location, there is a method call
     * of the expect name with the expected argument.
     */
    Optional<MethodCallExpr> readValueCallOpt =
        methodDeclaration.get().findAll(ExpressionStmt.class).stream()
            .filter(es -> es.getRange().isPresent())
            .filter(
                es -> RegionNodeMatcher.EXACT_MATCH.matches(lastEventLocation, es.getRange().get()))
            .filter(es -> es.getExpression() instanceof VariableDeclarationExpr)
            .map(es -> (VariableDeclarationExpr) es.getExpression())
            .map(vd -> vd.getVariable(0))
            .filter(vd -> vd.getInitializer().isPresent())
            .map(vd -> vd.getInitializer().get())
            .filter(init -> init instanceof MethodCallExpr)
            .map(init -> (MethodCallExpr) init)
            .filter(vd -> vd.getRange().isPresent())
            .filter(mc -> "readValue".equals(mc.getNameAsString()))
            .filter(mc -> mc.getArgument(0).toString().equals(variableName))
            .findFirst();

    if (readValueCallOpt.isEmpty()) {
      return false;
    }

    // we've successfully located the readValue() call -- first we update the argument
    MethodCallExpr readValueCall = readValueCallOpt.get();
    readValueCall.setArgument(0, stream);

    // now we remove the IOUtils#toString() assignment statement
    varDeclStmt.remove();
    return true;
  }
}
