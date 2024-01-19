package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.providers.sonar.api.Issue;

/**
 * A class that extends DefineConstantForLiteral and specializes in using an existing constant
 * suggested by an issue to replace string literals in Java code.
 */
final class UseExistingConstantForLiteral extends DefineConstantForLiteral {

  UseExistingConstantForLiteral(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {
    super(context, cu, stringLiteralExpr, issue);
  }

  /**
   * Retrieves the suggested constant name from the issue's message. The issue's message is expected
   * to follow a format such as "Use already-defined constant 'MY_CONSTANT' instead of duplicating
   * its value here." The method extracts the constant name found between the single quotes ('').
   */
  @Override
  protected String getConstantName() {
    final String message = issue.getMessage();
    int startIndex = message.indexOf("'");
    int endIndex = message.lastIndexOf("'");

    return message.substring(startIndex + 1, endIndex);
  }

  /**
   * This method is not implemented in this subclass, as it assumes the constant is already defined.
   * It remains empty in this implementation.
   */
  @Override
  protected void defineConstant(final String constantName) {
    // Empty implementation
  }
}
