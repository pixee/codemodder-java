package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.ast.ASTTransforms.removeImportIfUnused;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/**
 * A codemod to replace `@Controller` with `@RestController` and remove `@ResponseBody` annotations
 */
@Codemod(
    id = "sonar:java/rest-controller-s6833",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RestControllerCodemod
    extends SonarPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  @Inject
  public RestControllerCodemod(@ProvidedSonarScan(ruleId = "java:S6833") final RuleIssues issues) {
    super(issues, ClassOrInterfaceDeclaration.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final Issue issue) {

    final Optional<AnnotationExpr> controllerAnnotationOptional =
        classOrInterfaceDeclaration.getAnnotationByName("Controller");

    if (controllerAnnotationOptional.isPresent()) {
      replaceControllerToRestControllerAnnotation(cu, controllerAnnotationOptional.get());

      final Optional<AnnotationExpr> responseBodyClassOptional =
          classOrInterfaceDeclaration.getAnnotationByName("ResponseBody");

      if (responseBodyClassOptional.isPresent()) {
        final AnnotationExpr responseBodyClassAnnotation = responseBodyClassOptional.get();
        responseBodyClassAnnotation.remove();
      } else {
        removeResponseBodyAnnotationFromClassMethods(classOrInterfaceDeclaration);
      }

      removeImportIfUnused(cu, "org.springframework.web.bind.annotation.ResponseBody");

      return true;
    }

    return false;
  }

  private void replaceControllerToRestControllerAnnotation(
      final CompilationUnit cu, final AnnotationExpr controllerAnnotation) {
    final AnnotationExpr restControllerAnnotation = new MarkerAnnotationExpr("RestController");
    controllerAnnotation.replace(restControllerAnnotation);
    removeImportIfUnused(cu, "org.springframework.stereotype.Controller");
    addImportIfMissing(cu, "org.springframework.web.bind.annotation.RestController");
  }

  private void removeResponseBodyAnnotationFromClassMethods(
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
    final List<MethodDeclaration> methods = classOrInterfaceDeclaration.getMethods();
    if (methods != null && !methods.isEmpty()) {
      methods.forEach(
          method -> {
            final Optional<AnnotationExpr> responseBodyMethodOptional =
                method.getAnnotationByName("ResponseBody");
            if (responseBodyMethodOptional.isPresent()) {
              final AnnotationExpr responseBodyMethodAnnotation = responseBodyMethodOptional.get();
              responseBodyMethodAnnotation.remove();
            }
          });
    }
  }
}
