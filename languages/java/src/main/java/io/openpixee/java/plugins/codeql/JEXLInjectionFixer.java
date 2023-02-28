package io.openpixee.java.plugins.codeql;

import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Optional;
import org.apache.commons.jexl3.JexlExpression;

/**
 * A library that contains methods for automatically fixing JEXL injections detected by CodeQL's
 * rule "java/jexl-expression-injection" whenever possible.
 */
public final class JEXLInjectionFixer {

  /**
   * Detects if a {@link MethodCallExpr} evaluation of a {@link
   * JexlExpression#evaluate(org.apache.commons.jexl3.JexlContext)} can be sandboxed and tries to
   * fix it. Combines {@code isFixable} and {@code tryToFix}.
   */
  public static Optional<Integer> checkAndFix(final MethodCallExpr mce) {
    return Optional.empty();
  }

  /**
   * Returns true if there exists a local {@link JexlEngine} used to create and evaluate the
   * expression of {@code mce} that can be sandboxed.
   */
  public static boolean isFixable(final MethodCallExpr mce) {
    return false;
  }

  /** Tries to sandbox the {@link JexlEngine#create()} and returns its line if it does. */
  public static Optional<Integer> tryToFix(final MethodCallExpr mce) {
    return Optional.empty();
  }
}
