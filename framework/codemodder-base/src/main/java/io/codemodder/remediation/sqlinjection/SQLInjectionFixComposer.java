package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.expr.MethodCallExpr;

public final class SQLInjectionFixComposer {

  private SQLInjectionFixComposer() {}

  public static boolean checkAndFix(final MethodCallExpr methodCallExpr) {
    // Check if any data injection fixes apply
    var maybeFixed = new SQLParameterizer(methodCallExpr).checkAndFix();
    if (maybeFixed.isPresent()) {
      SQLParameterizerWithCleanup.cleanup(maybeFixed.get());
      SQLTableInjectionFilterTransform.findAndFix(maybeFixed.get());
      return true;
      // If not, try the table injection only
    } else {
      return SQLTableInjectionFilterTransform.findAndFix(methodCallExpr);
    }
  }

  public static boolean match(final MethodCallExpr methodCallExpr) {
    return SQLParameterizer.isSupportedJdbcMethodCall(methodCallExpr)
        || SQLTableInjectionFilterTransform.matchCall(methodCallExpr);
  }
}
