package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.*;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

/** A codemod to replace `@Controller` with `@RestController` and remove `@ResponseBody` annotations */
@Codemod(
    id = "sonar:java/rest-controller-s6833",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RestControllerCodemod
    extends SonarPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  @Inject
  public RestControllerCodemod(
      @ProvidedSonarScan(ruleId = "java:S6833") final RuleIssues issues) {
    super(issues, ClassOrInterfaceDeclaration.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final Issue issue) {


      final Optional<AnnotationExpr> controllerAnnotationOptional = classOrInterfaceDeclaration.getAnnotationByName("Controller");

        if(controllerAnnotationOptional.isPresent()){
            final AnnotationExpr restControllerAnnotation = new MarkerAnnotationExpr("RestController");
            final AnnotationExpr controllerAnnotation = controllerAnnotationOptional.get();
            controllerAnnotation.replace(restControllerAnnotation);

            final Optional<AnnotationExpr> responseBodyClassOptional = classOrInterfaceDeclaration.getAnnotationByName("ResponseBody");

            if(responseBodyClassOptional.isPresent()){
                removeAnnotation(responseBodyClassOptional);
            } else {
                removeResponseBodyAnnotationFromClassMethods(classOrInterfaceDeclaration);
            }

            return true;
        }


    return false;
  }

  private void removeResponseBodyAnnotationFromClassMethods(final ClassOrInterfaceDeclaration classOrInterfaceDeclaration){
      final List<MethodDeclaration> methods = classOrInterfaceDeclaration.getMethods();
      if(methods != null && !methods.isEmpty()){
          methods.forEach( method -> {
              final Optional<AnnotationExpr> responseBodyMethodOptional = method.getAnnotationByName("ResponseBody");
              if(responseBodyMethodOptional.isPresent()){
                  removeAnnotation(responseBodyMethodOptional);
              }
          });
      }
  }

  private void removeAnnotation(final Optional<AnnotationExpr> annotationExprOptional){
      final AnnotationExpr annotationExpr = annotationExprOptional.get();
      annotationExpr.remove();
  }
}
