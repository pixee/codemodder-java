package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Harden when the location points to a method call argument. */
final class HeaderInjectionFixMethodArgumentStrategy extends BaseHeaderInjectionFixStrategy {

  static boolean matchMethodArgument(final Node node) {
    return node instanceof Expression expression
        && ASTs.isArgumentOfMethodCall(expression)
            .filter(call -> call.hasScope() && setHeaderNames.contains(call.getNameAsString()))
            .filter(call -> call.getArguments().size() == 2)
            .filter(call -> !(call.getArgument(1) instanceof StringLiteralExpr))
            .filter(call -> call.getArgument(1) == node)
            .isPresent();
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    // have all the guarantees this is what we expect from the matcher call above
    Expression expression = (Expression) node;
    Optional<MethodCallExpr> maybeMethodCall = ASTs.isArgumentOfMethodCall(expression);
    MethodCallExpr setHeaderCall = maybeMethodCall.get();
    return fix(setHeaderCall, setHeaderCall.getArgument(1));
  }
}
