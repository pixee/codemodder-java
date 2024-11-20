package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/** Offers utilities for wrapping String expressions with an encoder. */
final class EncoderWrapping {

  private EncoderWrapping() {}

  @NotNull
  static SuccessOrReason fix(final NodeWithArguments<?> argument, final int index) {
    Optional<Expression> thingToWrap = findExpressionToWrap(argument.getArgument(index));
    if (thingToWrap.isEmpty()) {
      return SuccessOrReason.reason("Could not find recognize code shape to fix.");
    }
    wrapExpression(thingToWrap.get());
    return SuccessOrReason.success(List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
  }

  /**
   * We handle 4 expression code shapes. <code>
   *     print(user.getName());
   *     print("Hello, " + user.getName());
   *     print(user.getName() + ", hello!");
   *     print("Hello, " + user.getName() + ", hello!");
   * </code>
   *
   * <p>Note that we should only handle, for the tougher cases, string literals in combination with
   * the given expression. Note any other combination of expressions.
   */
  private static Optional<Expression> findExpressionToWrap(final Expression expression) {
    if (expression.isNameExpr()) {
      return Optional.of(expression);
    } else if (expression.isBinaryExpr()) {
      BinaryExpr binaryExpr = expression.asBinaryExpr();
      if (binaryExpr.getLeft().isBinaryExpr() && binaryExpr.getRight().isStringLiteralExpr()) {
        BinaryExpr leftBinaryExpr = binaryExpr.getLeft().asBinaryExpr();
        if (leftBinaryExpr.getLeft().isStringLiteralExpr()
            && !leftBinaryExpr.getRight().isStringLiteralExpr()) {
          return Optional.of(leftBinaryExpr.getRight());
        }
      } else if (binaryExpr.getLeft().isStringLiteralExpr()
          && binaryExpr.getRight().isStringLiteralExpr()) {
        return Optional.empty();
      } else if (binaryExpr.getLeft().isStringLiteralExpr()) {
        return Optional.of(binaryExpr.getRight());
      } else if (binaryExpr.getRight().isStringLiteralExpr()) {
        return Optional.of(binaryExpr.getLeft());
      }
    }
    return Optional.empty();
  }

  /** Wraps the given expression with the OWASP encoder. */
  private static void wrapExpression(final Expression expressionToWrap) {
    wrap(expressionToWrap).withStaticMethod("org.owasp.encoder.Encode", "forHtml", false);
  }
}
