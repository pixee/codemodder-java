package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

/**
 * Fix strategy for XSS vulnerabilities where a variable is returned directly and that is what's
 * vulnerable.
 */
final class NakedVariableReturnFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var maybeReturn = Optional.of(node).map(n -> n instanceof ReturnStmt ? (ReturnStmt) n : null);
    if (maybeReturn.isEmpty()) {
      return SuccessOrReason.reason("Not a return statement");
    }
    ReturnStmt nakedReturn = maybeReturn.get();
    wrap(nakedReturn.getExpression().get())
        .withStaticMethod("org.owasp.encoder.Encode", "forHtml", false);
    return SuccessOrReason.success(List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
  }

  static boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof ReturnStmt ? (ReturnStmt) n : null)
        .filter(rs -> rs.getExpression().isPresent())
        .filter(rs -> rs.getExpression().get().isNameExpr())
        .isPresent();
  }
}
