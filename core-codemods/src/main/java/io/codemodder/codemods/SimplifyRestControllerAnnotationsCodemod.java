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
    id = "sonar:java/simplify-rest-controller-annotations-s6833",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SimplifyRestControllerAnnotationsCodemod
    extends SonarPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  @Inject
  public SimplifyRestControllerAnnotationsCodemod(
      @ProvidedSonarScan(ruleId = "java:S6833") final RuleIssues issues) {
    super(issues, ClassOrInterfaceDeclaration.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final Issue issue) {

    final Optional<AnnotationExpr> controllerAnnotationOptional =
        classOrInterfaceDeclaration.getAnnotationByName("Controller");

    if (controllerAnnotationOptional.isEmpty()) {
      return false;
    }

    replaceControllerToRestControllerAnnotation(cu, controllerAnnotationOptional.get());

    final Optional<AnnotationExpr> responseBodyClassAnnotationOptional =
        classOrInterfaceDeclaration.getAnnotationByName("ResponseBody");

    responseBodyClassAnnotationOptional.ifPresent(AnnotationExpr::remove);

    removeResponseBodyAnnotationFromClassMethods(classOrInterfaceDeclaration);

    removeImportIfUnused(cu, "org.springframework.web.bind.annotation.ResponseBody");

    return true;
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
            final Optional<AnnotationExpr> responseBodyMethodAnnotationOptional =
                method.getAnnotationByName("ResponseBody");
            responseBodyMethodAnnotationOptional.ifPresent(AnnotationExpr::remove);
          });
    }
  }
}
