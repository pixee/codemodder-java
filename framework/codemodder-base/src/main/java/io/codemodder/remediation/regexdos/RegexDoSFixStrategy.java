package io.codemodder.remediation.regexdos;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.MatchAndFixStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

/** Adds a timeout function and wraps regex match call with it * */
final class RegexDoSFixStrategy extends MatchAndFixStrategy {

  /**
   * Test if the node is a Pattern.matcher*() call
   *
   * @param node
   * @return
   */
  @Override
  public boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr mce ? mce : null)
        .filter(mce -> mce.hasScope())
        // Check if the type is Pattern
        .filter(
            mce ->
                ASTs.calculateResolvedType(mce)
                    .filter(t -> "java.util.regex.Pattern".equals(t.describe()))
                    .isPresent())
        .filter(mce -> "matcher".equals(mce.getNameAsString()))
        .isPresent();
  }

  private static List<String> matchingMethods =
      List.of("matches", "find", "replaceAll", "replaceFirst");

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    // Find all the matcher calls from the matchingMethods list
    // if any, wrap it with executeWithTimeout with a default 5000 of timeout
    // Add executeWithTimout method to the encompassing class
    // Add needed imports (Callable, RuntimeException)
    return SuccessOrReason.reason("Doesn't match expected code shape");
  }
}
