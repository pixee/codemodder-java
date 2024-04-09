package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.ast.ASTTransforms.removeImportIfUnused;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/verbose-request-mapping",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class VerboseRequestMappingCodemod
    extends SarifPluginJavaParserChanger<NormalAnnotationExpr> implements FixOnlyCodeChanger {

  @Inject
  public VerboseRequestMappingCodemod(
      @SemgrepScan(ruleId = "verbose-request-mapping") final RuleSarif sarif) {
    super(sarif, NormalAnnotationExpr.class);
  }

  @Override
  public String vendorName() {
    return "Semgrep";
  }

  @Override
  public DetectorRule getDetectorRule() {
    return new DetectorRule(
        "verbose-request-mapping",
        "Replaced @RequestMapping annotation with shortcut annotation for requested HTTP Method",
        "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html");
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final NormalAnnotationExpr annotationExpr,
      final Result result) {

    // Find the http method if it exists
    Optional<MemberValuePair> method =
        annotationExpr.getPairs().stream()
            .filter(pair -> pair.getNameAsString().equals("method"))
            .findFirst();
    if (method.isPresent()) {
      MemberValuePair methodAttribute = method.get();
      // Store method and remove the "method" attribute
      String httpMethod = methodAttribute.getValue().toString();
      Optional<String> newType = getType(httpMethod);
      if (newType.isPresent()) {
        annotationExpr.getPairs().remove(methodAttribute);
        annotationExpr.setName(newType.get());
        addImportIfMissing(cu, "org.springframework.web.bind.annotation." + newType.get());
        removeImportIfUnused(cu, "org.springframework.web.bind.annotation.RequestMapping");
        removeImportIfUnused(cu, "org.springframework.web.bind.annotation.RequestMethod");
        return ChangesResult.changesApplied;
      }
    }
    return ChangesResult.noChanges;
  }

  public static Optional<String> getType(final String httpMethod) {
    String newType =
        switch (httpMethod) {
          case "RequestMethod.GET", "GET" -> "GetMapping";
          case "RequestMethod.PUT", "PUT" -> "PutMapping";
          case "RequestMethod.DELETE", "DELETE" -> "DeleteMapping";
          case "RequestMethod.POST", "POST" -> "PostMapping";
          case "RequestMethod.PATCH", "PATCH" -> "PatchMapping";
          default -> null;
        };
    return Optional.ofNullable(newType);
  }
}
