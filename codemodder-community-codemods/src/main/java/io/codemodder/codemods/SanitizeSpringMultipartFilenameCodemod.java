package io.codemodder.codemods;

import static io.codemodder.JavaParserTransformer.wrapExpression;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.Filenames;
import java.util.List;
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
    return wrapExpression(methodCallExpr)
        .withStaticMethod(Filenames.class.getName(), "toSimpleFileName");
  }
}
