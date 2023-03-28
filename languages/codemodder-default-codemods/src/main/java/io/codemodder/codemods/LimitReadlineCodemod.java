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
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.BoundedLineReader;
import java.util.List;
import javax.inject.Inject;

/** Turns hardcoded seeds for PRNGs to be more random. */
@Codemod(
    id = "pixee:java/limit-readline",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class LimitReadlineCodemod extends SemgrepJavaParserChanger<MethodCallExpr> {

  @Inject
  public LimitReadlineCodemod(@SemgrepScan(ruleId = "limit-readline") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr readLineCall,
      final Result result) {
    Node readLineParent = readLineCall.getParentNode().get();
    Expression readerScope = readLineCall.getScope().get();
    MethodCallExpr safeExpression =
        new MethodCallExpr(new NameExpr(BoundedLineReader.class.getSimpleName()), "readLine");
    safeExpression.setArguments(
        NodeList.nodeList(readerScope, new IntegerLiteralExpr(String.valueOf(defaultLineMaximum))));
    ASTTransforms.addImportIfMissing(cu, BoundedLineReader.class);
    readLineParent.replace(readLineCall, safeExpression);
    return true;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }

  private static final int defaultLineMaximum = 1_000_000; // 1 MB
}
