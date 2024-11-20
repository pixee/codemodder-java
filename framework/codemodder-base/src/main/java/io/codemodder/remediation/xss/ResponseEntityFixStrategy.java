package io.codemodder.remediation.xss;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/**
 * Fix strategy for XSS vulnerabilities where a variable/expr is sent to a Spring ResponseEntity.
 */
final class ResponseEntityFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof ObjectCreationExpr ? (ObjectCreationExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }

    ObjectCreationExpr call = maybeCall.get();
    return EncoderWrapping.fix(call, 0);
  }

  static boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof ObjectCreationExpr ? (ObjectCreationExpr) n : null)
        .filter(
            c ->
                "ResponseEntity".equals(c.getTypeAsString())
                    || c.getTypeAsString().startsWith("ResponseEntity<"))
        .filter(c -> !c.getArguments().isEmpty())
        .isPresent();
  }
}
