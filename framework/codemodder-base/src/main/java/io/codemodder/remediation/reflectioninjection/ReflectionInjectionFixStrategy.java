package io.codemodder.remediation.reflectioninjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import io.github.pixee.security.Reflection;
import java.util.List;
import java.util.Optional;

public final class ReflectionInjectionFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }

    MethodCallExpr methodCallExpr = maybeCall.get();
    replaceMethodCallExpression(cu, methodCallExpr);
    return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }

  /**
   * Updates the scope and name of the method call expression to {@code Reflection.loadAndVerify},
   * and adds the import if missing.
   *
   * @param cu CompilationUnit to update with imports
   * @param methodCallExpr the method call expression to update
   */
  private static void replaceMethodCallExpression(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
    final var name = new NameExpr(Reflection.class.getSimpleName());
    methodCallExpr.setScope(name);
    methodCallExpr.setName("loadAndVerify");
    addImportIfMissing(cu, Reflection.class);
  }
}
