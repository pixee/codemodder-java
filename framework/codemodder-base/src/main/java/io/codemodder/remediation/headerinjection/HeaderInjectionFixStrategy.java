package io.codemodder.remediation.headerinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.remediation.*;
import java.util.Optional;

public class HeaderInjectionFixStrategy implements RemediationStrategy {

  private static final String validatorMethodName = "stripNewlines";

  private static final String fixMethodCode =
      """
                    private static String stripNewlines(final String s) {
                      return s.replaceAll("[\\n\\r]", "");
                    }
                    """;

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeMethodCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeMethodCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }
    MethodCallExpr setHeaderCall = maybeMethodCall.get();
    Expression headerValueArgument = setHeaderCall.getArgument(1);
    wrap(headerValueArgument).withScopelessMethod(validatorMethodName);

    Optional<ClassOrInterfaceDeclaration> maybeParentClass =
        setHeaderCall.findAncestor(ClassOrInterfaceDeclaration.class);
    if (maybeParentClass.isEmpty()) {
      return SuccessOrReason.reason("Could not find parent class");
    }
    var parentClass = maybeParentClass.get();

    // add the validation method if it's not already present
    if (parentClass.isInterface()) {
      MethodCallExpr inlinedStripCall =
          new MethodCallExpr(
              headerValueArgument,
              "replaceAll",
              NodeList.nodeList(new StringLiteralExpr("[\\n\\r]"), new StringLiteralExpr("")));
      setHeaderCall.getArguments().set(1, inlinedStripCall);
    } else {
      boolean alreadyHasResourceValidationCallPresent =
          parentClass.findAll(MethodDeclaration.class).stream()
              .anyMatch(
                  md ->
                      md.getNameAsString().equals(validatorMethodName)
                          && md.getParameters().size() == 1
                          && md.getParameters().get(0).getTypeAsString().equals("String"));

      if (!alreadyHasResourceValidationCallPresent) {
        // one might be tempted to cache this result, but then it will be a shared resource with
        // shared CST metadata and cause bugs
        MethodDeclaration fixMethod = StaticJavaParser.parseMethodDeclaration(fixMethodCode);

        // add the method to the class
        parentClass.addMember(fixMethod);
      }
    }
    return SuccessOrReason.success();
  }
}
