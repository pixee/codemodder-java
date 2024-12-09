package io.codemodder.remediation.weakcrypto;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;

/** Changes the algorithm used in a MessageDigest.getInstance() call to SHA-256. */
final class MessageDigestFixer implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    MethodCallExpr getInstanceCall = (MethodCallExpr) node;
    // change the algorithm to SHA-256
    if (getInstanceCall.getArguments().size() != 1) {
      return SuccessOrReason.reason("getInstance() should have exactly one argument");
    }

    Expression argument = getInstanceCall.getArguments().get(0);
    argument.replace(new StringLiteralExpr("SHA-256"));
    return SuccessOrReason.success();
  }

  /** Check if the method call expression is a call to {@code MessageDigest.getInstance(String)}. */
  static boolean match(final Node node) {
    return node instanceof MethodCallExpr call
        && call.getNameAsString().equals("getInstance")
        && call.getArguments().isNonEmpty()
        && call.getScope().isPresent()
        && call.getScope().get().isNameExpr();
  }
}
