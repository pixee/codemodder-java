package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.openpixee.security.Filenames;
import java.util.List;
import javax.inject.Inject;

/** Sanitizes multipart filename inputs from HTTP requests. */
@Codemod(
    id = "pixee:java/sanitize-multipart-filename",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SanitizeMultipartFilenameCodemod
    extends SemgrepJavaParserChanger<MethodCallExpr> {
  @Inject
  public SanitizeMultipartFilenameCodemod(
      @SemgrepScan(ruleId = "sanitize-multipart-filename") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class, RegionExtractor.FROM_FIRST_THREADFLOW_EVENT);
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
  }

  @Override
  public void onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    Node parent = methodCallExpr.getParentNode().get();
    MethodCallExpr safeCall =
        new MethodCallExpr(
            new NameExpr(Filenames.class.getSimpleName()),
            "toSimpleFileName",
            NodeList.nodeList(methodCallExpr));
    parent.replace(methodCallExpr, safeCall);
    addImportIfMissing(cu, Filenames.class);
  }
}
