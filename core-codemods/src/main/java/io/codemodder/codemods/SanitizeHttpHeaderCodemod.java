package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.Newlines;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/strip-http-header-newlines",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SanitizeHttpHeaderCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public SanitizeHttpHeaderCodemod(
      @SemgrepScan(ruleId = "strip-http-header-newlines") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr setHeaderCall,
      final Result result) {
    Expression headerValueArgument = setHeaderCall.getArgument(1);
    return wrap(headerValueArgument).withStaticMethod(Newlines.class.getName(), "stripAll", false);
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }
}
