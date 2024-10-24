package io.codemodder.remediation.weakrandom;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/** Fixes weak random vulnerabilities */
final class WeakRandomFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeOCE =
        Optional.of(node).map(n -> n instanceof ObjectCreationExpr ? (ObjectCreationExpr) n : null);
    if (maybeOCE.isEmpty()) {
      return SuccessOrReason.reason("Not an object creation expression");
    }
    ObjectCreationExpr unsafeRandom = maybeOCE.get();
    unsafeRandom.setType("SecureRandom");
    addImportIfMissing(cu, SecureRandom.class.getName());
    return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }
}
