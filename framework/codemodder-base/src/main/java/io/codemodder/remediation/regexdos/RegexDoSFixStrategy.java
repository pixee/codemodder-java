package io.codemodder.remediation.regexdos;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import io.codemodder.DependencyGAV;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.remediation.MatchAndFixStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

/** Adds a timeout function and wraps regex match call with it * */
final class RegexDoSFixStrategy extends MatchAndFixStrategy {

  private final String DEFAULT_TIMEOUT = "5000";

  private static final List<String> matchingMethods =
      List.of("matches", "find", "replaceAll", "replaceFirst");

  /**
   * Test if the node is an argument of a Pattern.matcher*() call
   *
   * @param node
   * @return
   */
  @Override
  public boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof Expression e ? e : null)
        .flatMap(ASTs::isArgumentOfMethodCall)
        .filter(mce -> "matcher".equals(mce.getNameAsString()))
        .flatMap(mce -> mce.getScope())
        // Check if the type is Pattern
        .filter(
            scope ->
                ASTs.calculateResolvedType(scope)
                    .filter(t -> "java.util.regex.Pattern".equals(t.describe()))
                    .isPresent())
        .isPresent();
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    // indirect case, assigned to a variable
    // We know this to be a Pattern.matcher() call from the match method
    MethodCallExpr call = (MethodCallExpr) ASTs.isArgumentOfMethodCall((Expression) node).get();
    var allValidMethodCalls =
        ASTs.isInitExpr(call).flatMap(LocalVariableDeclaration::fromVariableDeclarator).stream()
            .flatMap(LocalDeclaration::findAllMethodCalls)
            .filter(mce -> matchingMethods.contains(mce.getNameAsString()))
            .toList();
    if (allValidMethodCalls.isEmpty()) {
      return SuccessOrReason.reason("Couldn't find any matching methods");
    }

    for (var mce : allValidMethodCalls) {
      // Wrap it with executeWithTimeout with a default 5000 of timeout
      var newCall =
          new MethodCallExpr(
              new NameExpr("ExecuteWithTimeout"),
              "executeWithTimeout",
              new NodeList<>(
                  new LambdaExpr(new NodeList<>(), mce.clone()),
                  new IntegerLiteralExpr(DEFAULT_TIMEOUT)));
      mce.replace(newCall);
    }

    ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.ExecuteWithTimeout");
    return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }
}
