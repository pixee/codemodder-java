package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.ast.ASTTransforms;

public final class SQLParameterizerWithCleanup {

  private SQLParameterizerWithCleanup() {}

  public static boolean checkAndFix(final MethodCallExpr methodCallExpr) {
    var maybeFixed = new SQLParameterizer(methodCallExpr).checkAndFix();
    if (maybeFixed.isPresent()) {
      // Cleanup
      var maybeMethodDecl = methodCallExpr.findAncestor(CallableDeclaration.class);

      // Remove concatenation with empty strings e.g "first" +  "" -> "first";
      maybeMethodDecl.ifPresent(ASTTransforms::removeEmptyStringConcatenation);
      // Remove potential unused variables left after transform
      maybeMethodDecl.ifPresent(md -> ASTTransforms.removeUnusedLocalVariables(md));

      // Merge concatenated literals, e.g. "first" + " and second" -> "first and second"
      maybeFixed
          .flatMap(mce -> mce.getArguments().getFirst())
          .ifPresent(ASTTransforms::mergeConcatenatedLiterals);
      return true;
    }
    return false;
  }
}
