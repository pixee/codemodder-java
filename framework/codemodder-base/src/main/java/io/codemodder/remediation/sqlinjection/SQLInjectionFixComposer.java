package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Composes several transformations related to SQL injections. */
public final class SQLInjectionFixComposer implements RemediationStrategy {

  SQLInjectionFixComposer() {}

  /**
   * Given a node, check if it is a {@link MethodCallExpr} related to executing JDBC API SQL queries
   * (i.e. prepareStatement(), executeQuery(), etc.), parameterize data injections or add a
   * validation step for structural injections.
   */
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    var maybeMethodCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeMethodCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }

    MethodCallExpr methodCallExpr = maybeMethodCall.get();

    // First, check if any data injection fixes apply
    var maybeFixed = new SQLParameterizer(methodCallExpr, cu).checkAndFix();
    if (maybeFixed.isPresent()) {
      // If yes, execute cleanup steps and check if any table injection remains.
      SQLParameterizerWithCleanup.cleanup(maybeFixed.get());
      SQLTableInjectionFilterTransform.findAndFix(maybeFixed.get());
      return SuccessOrReason.success();
      // If not, try the table injection only
    } else {
      return SQLTableInjectionFilterTransform.findAndFix(methodCallExpr)
          ? SuccessOrReason.success()
          : SuccessOrReason.reason("Could not fix injection");
    }
  }

  /**
   * Check if the node is a JDBC API query method that is a target of a SQL injection
   * transformation.
   */
  public static boolean match(final Node node) {
    var maybeMethodCall =
        Optional.of(node)
            .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
            .filter(
                n ->
                    SQLParameterizer.isSupportedJdbcMethodCall(n)
                        || SQLTableInjectionFilterTransform.matchCall(n));
    return maybeMethodCall.isPresent();
  }
}
