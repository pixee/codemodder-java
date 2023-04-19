package io.codemodder.codemods;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/optimize-jackson-string-usage",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
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

    // We start by obtaining the String declaration
    VariableDeclarationExpr varDeclExpr = varDeclStmt.getExpression().asVariableDeclarationExpr();
    VariableDeclarator variable = varDeclExpr.getVariable(0);
    String variableName = variable.getNameAsString();

    // Since we want to delete the string, we need to check if it is only used once at readValue
    List<NameExpr> allVariableReferences =
        LocalVariableDeclaration.fromVariableDeclarator(variable).stream()
            .flatMap(lvd -> ASTs.findAllReferences(lvd).stream())
            .collect(Collectors.toList());
    if (allVariableReferences.size() != 1) {
      return false;
    }

    // We now look for the object declaration
    Optional<MethodDeclaration> methodDeclaration =
        varDeclStmt.findAncestor(MethodDeclaration.class);
    if (methodDeclaration.isEmpty()) {
      return false;
    }
    // This get() is safe because of the semgrep rule
    Expression stream = variable.getInitializer().get().asMethodCallExpr().getArgument(0);
    List<ThreadFlowLocation> threadFlow =
        result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations();
    PhysicalLocation lastLocation =
        threadFlow.get(threadFlow.size() - 1).getLocation().getPhysicalLocation();
    Region lastRegion = lastLocation.getRegion();

    var maybeObjectDeclaration =
        methodDeclaration.get().findAll(ExpressionStmt.class).stream()
            .filter(es -> es.getRange().isPresent())
            .filter(es -> RegionNodeMatcher.EXACT_MATCH.matches(lastRegion, es.getRange().get()))
            .findFirst();

    // useless but here for robustness
    if (maybeObjectDeclaration.isEmpty()) {
      return false;
    }

    /*
     * This check is quite robust -- it makes sure that at the last SARIF event location, there is a method call
     * of the expect name with the expected argument.
     */
    Optional<MethodCallExpr> readValueCallOpt =
        maybeObjectDeclaration
            .filter(es -> es.getExpression() instanceof VariableDeclarationExpr)
            .map(es -> (VariableDeclarationExpr) es.getExpression())
            .map(vd -> vd.getVariable(0))
            .flatMap(vd -> vd.getInitializer())
            .filter(init -> init instanceof MethodCallExpr)
            .map(init -> (MethodCallExpr) init)
            .filter(vd -> vd.getRange().isPresent())
            .filter(mc -> "readValue".equals(mc.getNameAsString()))
            .filter(mc -> mc.getArgument(0).toString().equals(variableName));

    if (readValueCallOpt.isEmpty()) {
      return false;
    }

    // All the checks have passed, we begin the fix
    MethodCallExpr readValueCall = readValueCallOpt.get();

    // We've successfully located the readValue() call -- first we update the argument
    readValueCall.setArgument(0, stream);

    // now we remove the IOUtils#toString() assignment statement
    varDeclStmt.remove();

    return true;
  }
}
