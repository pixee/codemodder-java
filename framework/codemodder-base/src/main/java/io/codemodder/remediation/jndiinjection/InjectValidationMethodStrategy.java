package io.codemodder.remediation.jndiinjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.DependencyGAV;
import java.util.List;
import java.util.Set;

/**
 * {@inheritDoc}
 *
 * <p>Fixes by injecting a validation method into the class and calling it before the lookup() call.
 */
final class InjectValidationMethodStrategy implements JNDIFixStrategy {

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
  public List<DependencyGAV> fix(
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration parentClass,
      final MethodCallExpr lookupCall,
      final NameExpr contextNameVariable,
      final BlockStmt blockStmt,
      final int index) {
    MethodCallExpr validationCall = new MethodCallExpr(null, validateResourceMethodName);
    validationCall.addArgument(contextNameVariable);
    blockStmt.addStatement(index, validationCall);

    // add the validation method if it's not already present
    boolean alreadyHasResourceValidationCallPresent =
        parentClass.findAll(MethodDeclaration.class).stream()
            .anyMatch(
                md ->
                    md.getNameAsString().equals(validateResourceMethodName)
                        && md.getParameters().size() == 1
                        && md.getParameters().get(0).getTypeAsString().equals("String"));

    if (!alreadyHasResourceValidationCallPresent) {
      parentClass.addMember(fixMethod);
      addImportIfMissing(cu, Set.class);
    }

    return List.of();
  }

  private static final String validateResourceMethodName = "validateResourceName";
}
