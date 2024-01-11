package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.providers.sonar.api.Issue;

public class UseExistingConstantForLiteral extends DefineConstantForLiteral {

  UseExistingConstantForLiteral(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {
    super(context, cu, stringLiteralExpr, issue);
  }

  @Override
  protected String getConstantName() {
    return getDefinedConstantValueInMessage(issue.getMessage());
  }

  @Override
  protected void defineConstant(String constantName) {
    // Empty implementation
  }

  // TODO
  public static String getDefinedConstantValueInMessage(final String message) {
    // Find the first occurrence of single quote
    int startIndex = message.indexOf("'");
    if (startIndex != -1) {
      // Find the next occurrence of single quote starting from the position after the first quote
      int endIndex = message.indexOf("'", startIndex + 1);
      if (endIndex != -1) {
        // Extract the value between the single quotes
        return message.substring(startIndex + 1, endIndex);
      }
    }
    // Return an empty string if no single quotes were found
    return "";
  }
}
