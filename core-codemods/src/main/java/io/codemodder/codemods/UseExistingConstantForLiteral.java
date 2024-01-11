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
  protected void defineConstant(final String constantName) {
    // Empty implementation
  }

  private String getDefinedConstantValueInMessage(final String message) {
    int startIndex = message.indexOf("'");
    int endIndex = message.lastIndexOf("'");

    return message.substring(startIndex + 1, endIndex);
  }
}
