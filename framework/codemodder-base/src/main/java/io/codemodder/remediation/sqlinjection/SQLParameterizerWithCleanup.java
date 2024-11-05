package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.ast.ASTTransforms;

public final class SQLParameterizerWithCleanup {

  private SQLParameterizerWithCleanup() {}

  public static boolean checkAndFix(final MethodCallExpr methodCallExpr) {
    var maybeFixed = new SQLParameterizer(methodCallExpr).checkAndFix();
    maybeFixed.ifPresent(call -> cleanup(call));
    return maybeFixed.isPresent();
  }

  public static void cleanup(final MethodCallExpr pstmtCall) {
    var maybeMethodDecl = pstmtCall.findAncestor(CallableDeclaration.class);

    // Remove concatenation with empty strings e.g "first" +  "" -> "first";
    maybeMethodDecl.ifPresent(ASTTransforms::removeEmptyStringConcatenation);
    // Remove potential unused variables left after transform
    // maybeMethodDecl.ifPresent(md -> ASTTransforms.removeUnusedLocalVariables(md));

    // Merge concatenated literals, e.g. "first" + " and second" -> "first and second"
    pstmtCall.getArguments().getFirst().ifPresent(ASTTransforms::mergeConcatenatedLiterals);
  }
}
