package io.codemodder.remediation.headerinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;
import java.util.Set;

/** Base class for header injection fix strategies. */
abstract class BaseHeaderInjectionFixStrategy implements RemediationStrategy {

  protected BaseHeaderInjectionFixStrategy() {}

  private static final String validatorMethodName = "stripNewlines";

  private static final String fixMethodCode =
      """
                          private static String stripNewlines(final String s) {
                            return s.replaceAll("[\\n\\r]", "");
                          }
                          """;

  protected static final Set<String> setHeaderNames = Set.of("setHeader", "addHeader");

  /**
   * Fix the header injection issue.
   *
   * @param setHeaderCall the method call to fix
   * @param headerValueArgument the argument to fix
   * @return success or reason
   */
  protected SuccessOrReason fix(
      final MethodCallExpr setHeaderCall, final Expression headerValueArgument) {
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
      wrap(headerValueArgument).withScopelessMethod(validatorMethodName);
      boolean alreadyHasResourceValidationCallPresent =
          isAlreadyHasResourceValidationCallPresent(parentClass);
      if (!alreadyHasResourceValidationCallPresent) {
        addValidationCall(parentClass);
      }
    }
    return SuccessOrReason.success();
  }

  private static boolean isAlreadyHasResourceValidationCallPresent(
      final ClassOrInterfaceDeclaration parentClass) {
    return parentClass.findAll(MethodDeclaration.class).stream()
        .anyMatch(
            md ->
                md.getNameAsString().equals(validatorMethodName)
                    && md.getParameters().size() == 1
                    && md.getParameters().get(0).getTypeAsString().equals("String"));
  }

  private static void addValidationCall(final ClassOrInterfaceDeclaration parentClass) {
    // one might be tempted to cache this result, but then it will be a shared resource with
    // shared CST metadata and cause bugs
    MethodDeclaration fixMethod = StaticJavaParser.parseMethodDeclaration(fixMethodCode);

    // add the method to the class
    parentClass.addMember(fixMethod);
  }
}
