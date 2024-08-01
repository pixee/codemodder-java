package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.expr.MethodCallExpr;

/** Composes several transformations related to SQL injections. */
public final class SQLInjectionFixComposer {

  private SQLInjectionFixComposer() {}

  /**
   * Given a {@link MethodCallExpr} related to executing JDBC API SQL queries (i.e.
   * prepareStatement(), executeQuery(), etc.), parameterize data injections or add a validation
   * step for structural injections.
   */
  public static boolean checkAndFix(final MethodCallExpr methodCallExpr) {
    // First, check if any data injection fixes apply
    var maybeFixed = new SQLParameterizer(methodCallExpr).checkAndFix();
    if (maybeFixed.isPresent()) {
      // If yes, execute cleanup steps and check if any table injection remains.
      SQLParameterizerWithCleanup.cleanup(maybeFixed.get());
      SQLTableInjectionFilterTransform.findAndFix(maybeFixed.get());
      return true;
      // If not, try the table injection only
    } else {
      return SQLTableInjectionFilterTransform.findAndFix(methodCallExpr);
    }
  }

  /**
   * Check if the {@link MethodCallExpr} is a JDBC API query method that is a target of a SQL
   * injection transformation.
   */
  public static boolean match(final MethodCallExpr methodCallExpr) {
    return SQLParameterizer.isSupportedJdbcMethodCall(methodCallExpr)
        || SQLTableInjectionFilterTransform.matchCall(methodCallExpr);
  }
}
