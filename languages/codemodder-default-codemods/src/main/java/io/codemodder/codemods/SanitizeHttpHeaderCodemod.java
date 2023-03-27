package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.openpixee.security.Newlines;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/strip-http-header-newlines",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SanitizeHttpHeaderCodemod extends SemgrepJavaParserChanger<MethodCallExpr> {

  @Inject
  public SanitizeHttpHeaderCodemod(
      @SemgrepScan(ruleId = "strip-http-header-newlines") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class);
  }

  @Override
  public void onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr setHeaderCall,
      final Result result) {
    Expression headerValueArgument = setHeaderCall.getArgument(1);
    MethodCallExpr safeCall =
        new MethodCallExpr(
            new NameExpr(Newlines.class.getSimpleName()),
            "stripAll",
            NodeList.nodeList(headerValueArgument));
    setHeaderCall.setArgument(1, safeCall);
    addImportIfMissing(cu, Newlines.class);
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
  }
}
