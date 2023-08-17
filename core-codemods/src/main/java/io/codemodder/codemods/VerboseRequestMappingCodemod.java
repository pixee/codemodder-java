package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/** Suggests verbose use of @RequestMapping to specify a method attribute. * */
@Codemod(
    id = "pixee:java/verbose-request-mapping",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class VerboseRequestMappingCodemod
    extends SarifPluginJavaParserChanger<AnnotationExpr> {

  @Inject
  public VerboseRequestMappingCodemod(
      @SemgrepScan(ruleId = "verbose-request-mapping") final RuleSarif sarif) {
    super(sarif, AnnotationExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final AnnotationExpr annotationExpr,
      final Result result) {

    if (annotationExpr instanceof NormalAnnotationExpr) {
      NormalAnnotationExpr normalAnnotationExpr = (NormalAnnotationExpr) annotationExpr;
      normalAnnotationExpr.getPairs().removeIf(pair -> pair.getNameAsString().equals("method"));
    }
    annotationExpr.setName("GetMapping");
    addImportIfMissing(cu, "org.springframework.web.bind.annotation.GetMapping");
    return true;
  }
}
