package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

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

    if (annotationExpr instanceof NormalAnnotationExpr normalAnnotationExpr) {

      // Find the http method if it exists
      MemberValuePair methodAttribute = null;
      for (MemberValuePair pair : normalAnnotationExpr.getPairs()) {
        if (pair.getNameAsString().equals("method")) {
          methodAttribute = pair;
          break;
        }
      }

      if (methodAttribute != null) {
        // Store method and remove the "method" attribute
        String httpMethod = methodAttribute.getValue().toString();
        normalAnnotationExpr.getPairs().remove(methodAttribute);

        String newType = getType(annotationExpr, httpMethod);
        annotationExpr.setName(newType);
        addImportIfMissing(cu, "org.springframework.web.bind.annotation." + newType);
        return true;
      }
    }
    return false;
  }

  public static String getType(AnnotationExpr annotationExpr, String httpMethod) {
    String newType =
        switch (httpMethod) {
          case "RequestMethod.GET", "GET" -> "GetMapping";
          case "RequestMethod.PUT", "PUT" -> "PutMapping";
          case "RequestMethod.DELETE", "DELETE" -> "DeleteMapping";
          case "RequestMethod.POST", "POST" -> "PostMapping";
          case "RequestMethod.PATCH", "PATCH" -> "PatchMapping";
          default -> "";
        };
    return newType;
  }
}
