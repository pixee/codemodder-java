package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.BoundedLineReader;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

/** Turns hardcoded seeds for PRNGs to be more random. */
@Codemod(
    id = "pixee:java/limit-readline",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class LimitReadlineCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private final Parameter limit;

  @Inject
  public LimitReadlineCodemod(
      @SemgrepScan(ruleId = "limit-readline") final RuleSarif sarif,
      @CodemodParameter(
              question =
                  "What is the maximum number of characters that should be allowed to be read from the reader?",
              name = "limit",
              type = CodemodParameter.ParameterType.NUMBER,
              label = "a positive integer",
              defaultValue = "5_000_000", // representing roughly 5MB
              validationPattern = "[0-9\\_]+")
          final Parameter limit) {
    super(sarif, MethodCallExpr.class);
    this.limit = Objects.requireNonNull(limit);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr readLineCall,
      final Result result) {
    Node readLineParent = readLineCall.getParentNode().get();
    Expression readerScope = readLineCall.getScope().get();
    MethodCallExpr safeExpression =
        new MethodCallExpr(new NameExpr(BoundedLineReader.class.getSimpleName()), "readLine");
    int line = readLineCall.getRange().get().begin.line;
    String stringLimitValue = limit.getValue(context.path(), line);
    safeExpression.setArguments(
        NodeList.nodeList(readerScope, new IntegerLiteralExpr(stringLimitValue)));
    ASTTransforms.addImportIfMissing(cu, BoundedLineReader.class);
    readLineParent.replace(readLineCall, safeExpression);
    return true;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }
}
