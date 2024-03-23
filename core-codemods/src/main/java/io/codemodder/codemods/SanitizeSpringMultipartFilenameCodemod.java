package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.Filenames;
import java.util.List;
import javax.inject.Inject;

/** Sanitizes multipart filename inputs from HTTP requests. */
@Codemod(
    id = "pixee:java/sanitize-spring-multipart-filename",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SanitizeSpringMultipartFilenameCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {
  @Inject
  public SanitizeSpringMultipartFilenameCodemod(
      @SemgrepScan(ruleId = "sanitize-spring-multipart-filename") RuleSarif semgrepSarif) {
    super(
        semgrepSarif,
        MethodCallExpr.class,
        SourceCodeRegionExtractor.FROM_SARIF_FIRST_THREADFLOW_EVENT);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    return wrap(methodCallExpr)
            .withStaticMethod(Filenames.class.getName(), "toSimpleFileName", false)
        ? ChangesResult.changesApplied(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT))
        : ChangesResult.noChanges();
  }
}
