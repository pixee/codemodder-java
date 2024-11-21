package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.remediation.*;

/** Harden when the location points to a method call itself. */
final class HeaderInjectionFixMethodCallStrategy extends BaseHeaderInjectionFixStrategy {

  static boolean matchMethodCall(final Node node) {
    return node instanceof MethodCallExpr call
        && call.hasScope()
        && setHeaderNames.contains(call.getNameAsString())
        && call.getArguments().size() == 2
        && !(call.getArgument(1) instanceof StringLiteralExpr);
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    MethodCallExpr methodCall = (MethodCallExpr) node;
    return fix(methodCall, methodCall.getArgument(1));
  }
}
