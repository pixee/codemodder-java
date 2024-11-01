package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.VisibleForTesting;

final class PrintingMethodFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }
    MethodCallExpr call = maybeCall.get();
    wrap(call.getArgument(0)).withStaticMethod("org.owasp.encoder.Encode", "forHtml", false);
    return SuccessOrReason.success(List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
  }

  private static final Set<String> writingMethodNames = Set.of("print", "println", "write");

  @VisibleForTesting
  public static boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(mce -> writingMethodNames.contains(mce.getNameAsString()))
        .filter(mce -> mce.getArguments().size() == 1)
        .isPresent();
  }
}
