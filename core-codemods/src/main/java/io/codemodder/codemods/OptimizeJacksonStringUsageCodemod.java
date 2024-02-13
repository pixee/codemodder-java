package io.codemodder.codemods;

import static io.codemodder.RegionNodeMatcher.EXACT_MATCH;
import static io.codemodder.Sarif.getLastDataFlowRegion;
import static io.codemodder.javaparser.ASTExpectations.expect;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/optimize-jackson-string-usage",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class OptimizeJacksonStringUsageCodemod
    extends SarifPluginJavaParserChanger<ExpressionStmt> {

  @Inject
  public OptimizeJacksonStringUsageCodemod(
      @SemgrepScan(ruleId = "optimize-jackson-string-usage") RuleSarif semgrepSarif) {
    super(
        semgrepSarif,
        ExpressionStmt.class,
        SourceCodeRegionExtractor.FROM_SARIF_FIRST_THREADFLOW_EVENT);
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
   * We've configured the {@link SourceCodeRegionExtractor} to pull the first data flow event, which
   * is the IOUtils#toString() call.
   */
  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ExpressionStmt varDeclStmt,
      final Result result) {

    Optional<MethodCallExpr> toStringCall =
        expect(varDeclStmt)
            // make sure it's not in a try-with-resources, foreach declaration, etc
            .withBlockParent()
            // make sure it's a variable declaration statement
            .toBeVariableDeclarationStatement()
            // make sure it's local and not a multi-variable declaration
            .toBeSingleLocalVariableDefinition()
            // make sure its only used the one place we expect
            .withDirectReferenceCount(1)
            // make sure it's a method call initializer
            .toBeInitializedByMethodCall()
            .result();

    if (toStringCall.isEmpty()) {
      return false;
    }
    String streamVariableName = toStringCall.get().getArgument(0).asNameExpr().getNameAsString();

    Region lastRegion = getLastDataFlowRegion(result);
    Optional<MethodCallExpr> readValueCallOpt =
        ASTs.findMethodBodyFrom(varDeclStmt).get().findAll(ExpressionStmt.class).stream()
            .filter(stmt -> stmt.getRange().isPresent())
            .filter(
                stmt ->
                    EXACT_MATCH.matches(
                        SourceCodeRegion.fromSarif(lastRegion), stmt.getRange().get()))
            .map(stmt -> stmt.getExpression().asVariableDeclarationExpr())
            .map(vde -> vde.getVariable(0).getInitializer().get().asMethodCallExpr())
            .findFirst();

    // just for robustness, but should never happen
    if (readValueCallOpt.isEmpty()) {
      return false;
    }

    // All the checks have passed, we begin the fix
    MethodCallExpr readValueCall = readValueCallOpt.get();

    // We've successfully located the readValue() call -- first we update the argument
    readValueCall.setArgument(0, new NameExpr(streamVariableName));

    // now we remove the IOUtils#toString() assignment statement
    varDeclStmt.remove();

    return true;
  }
}
