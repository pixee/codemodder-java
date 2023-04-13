package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.Filenames;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Sanitizes multipart filename inputs from HTTP requests. */
@Codemod(
    id = "pixee:java/sanitize-spring-multipart-filename",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SanitizeSpringMultipartFilenameCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {
  @Inject
  public SanitizeSpringMultipartFilenameCodemod(
      @SemgrepScan(ruleId = "sanitize-spring-multipart-filename") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class, RegionExtractor.FROM_FIRST_THREADFLOW_EVENT);
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    Optional<Node> parentNode = methodCallExpr.getParentNode();
    if (parentNode.isPresent()) {
      Node parent = parentNode.get();
      MethodCallExpr safeCall =
          new MethodCallExpr(
              new NameExpr(Filenames.class.getSimpleName()),
              "toSimpleFileName",
              NodeList.nodeList(methodCallExpr));
      parent.replace(methodCallExpr, safeCall);
      addImportIfMissing(cu, Filenames.class);
      return true;
    }
    return false;
  }
}
