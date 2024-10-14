package io.codemodder.remediation.jndiinjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * <p>Fixes by injecting a validation method into the class and calling it before the lookup() call.
 */
final class InjectValidationMethodStrategy implements RemediationStrategy {

  private final MethodDeclaration fixMethod;

  InjectValidationMethodStrategy() {
    String fixMethodCode =
        """
                        private static void validateResourceName(final String name) {
                            if (name != null) {
                              Set<String> illegalNames = Set.of("ldap://", "rmi://", "dns://", "java:");
                              String canonicalName = name.toLowerCase().trim();
                              if (illegalNames.stream().anyMatch(canonicalName::startsWith)) {
                                throw new SecurityException("Illegal JNDI resource name: " + name);
                              }
                            }
                        }
                        """;
    this.fixMethod = StaticJavaParser.parseMethodDeclaration(fixMethodCode);
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var contextOrReason = JNDIFixContext.fromNode(node);

    if (contextOrReason.isRight()) {
      return SuccessOrReason.reason(contextOrReason.getRight());
    }

    var context = contextOrReason.getLeft();
    MethodCallExpr validationCall = new MethodCallExpr(null, validateResourceMethodName);
    validationCall.addArgument(context.contextNameVariable());
    context.blockStmt().addStatement(context.index(), validationCall);

    // add the validation method if it's not already present
    boolean alreadyHasResourceValidationCallPresent =
        context.parentClass().findAll(MethodDeclaration.class).stream()
            .anyMatch(
                md ->
                    md.getNameAsString().equals(validateResourceMethodName)
                        && md.getParameters().size() == 1
                        && md.getParameters().get(0).getTypeAsString().equals("String"));

    if (!alreadyHasResourceValidationCallPresent) {
      context.parentClass().addMember(fixMethod);
      addImportIfMissing(cu, Set.class);
    }

    return SuccessOrReason.success();
  }

  private static final String validateResourceMethodName = "validateResourceName";
}
