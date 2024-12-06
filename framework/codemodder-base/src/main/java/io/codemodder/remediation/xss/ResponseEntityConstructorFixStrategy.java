package io.codemodder.remediation.xss;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/**
 * Fix strategy for XSS vulnerabilities where a variable/expr is sent to a Spring ResponseEntity
 * constructor.
 */
final class ResponseEntityConstructorFixStrategy implements RemediationStrategy {

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
        .filter(
            c -> {
              Expression firstArg = c.getArguments().getFirst().get();
              try {
                ResolvedType resolvedType = firstArg.calculateResolvedType();
                return "java.lang.String".equals(resolvedType.describe());
              } catch (Exception e) {
                // this is expected often, and indicates its a non-String type anyway
                return false;
              }
            })
        .isPresent();
  }
}
