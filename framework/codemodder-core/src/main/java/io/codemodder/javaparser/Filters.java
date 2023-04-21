package io.codemodder.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.ast.ASTPatterns;

/**
 * A utility for making it easy to filter JavaParser ASTs.
 */
public final class Filters {

  private Filters() {}

  public static boolean isInMethodBody(final Node node) {
    return node.findAncestor(MethodDeclaration.class).isPresent();
  }

  public static boolean isBlockVariableDeclarationAndAssignment(final ExpressionStmt stmt) {
    if (!ASTPatterns.isInBlock(stmt).isPresent()) {
      return false;
    }
    Expression expression = stmt.getExpression();
    if (!expression.isVariableDeclarationExpr()) {
      return false;
    }
    VariableDeclarationExpr declarationExpr = expression.asVariableDeclarationExpr();
    return declarationExpr.getVariables().size() == 1
        && declarationExpr.getVariable(0).getInitializer().isPresent();
  }

  public static boolean isVariableReferencedExactly(final VariableDeclarator vd, final int times) {
    long count =
        vd.findAncestor(MethodDeclaration.class).get().findAll(NameExpr.class).stream()
            .filter(n -> n.getNameAsString().equals(vd.getNameAsString()))
            .count();
    return count == times;
  }
}
