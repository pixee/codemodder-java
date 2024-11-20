package io.codemodder.remediation.xss;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;
import java.util.Set;

/** Fix strategy for XSS vulnerabilities where a variable is sent to a simple print/write call. */
final class PrintingMethodFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }
    MethodCallExpr call = maybeCall.get();
    return EncoderWrapping.fix(call, 0);
  }

  private static final Set<String> writingMethodNames = Set.of("print", "println", "write");

  static boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(mce -> writingMethodNames.contains(mce.getNameAsString()))
        .filter(mce -> mce.getArguments().size() == 1)
        .isPresent();
  }
}
