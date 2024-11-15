package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
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

    Expression methodArgument = call.getArgument(0);

    Optional<Expression> thingToWrap = findExpressionToWrap(methodArgument);
    if (thingToWrap.isEmpty()) {
      return SuccessOrReason.reason("Could not find recognize code shape to fix.");
    }
    Expression expressionToWrap = thingToWrap.get();
    wrap(expressionToWrap).withStaticMethod("org.owasp.encoder.Encode", "forHtml", false);
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
  private Optional<Expression> findExpressionToWrap(final Expression expression) {
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

  private static final Set<String> writingMethodNames = Set.of("print", "println", "write");

  static boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(mce -> writingMethodNames.contains(mce.getNameAsString()))
        .filter(mce -> mce.getArguments().size() == 1)
        .isPresent();
  }
}
