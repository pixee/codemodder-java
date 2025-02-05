package io.codemodder.remediation.pathtraversal;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import io.github.pixee.security.Filenames;

/**
 * Fix strategy for Spring MultipartFile getOriginalFilename() calls which wraps with
 * java-security-toolkit API for guaranteeing a simple file name.
 */
final class SpringMultipartFixStrategy implements RemediationStrategy {
  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    MethodCallExpr methodCallExpr = (MethodCallExpr) node;
    boolean success =
        wrap(methodCallExpr).withStaticMethod(Filenames.class.getName(), "toSimpleFileName", false);
    return success ? SuccessOrReason.success() : SuccessOrReason.reason("Wrap failed");
  }
}
