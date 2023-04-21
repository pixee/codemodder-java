package io.codemodder.javaparser;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

/**
 * A utility for making it easy to transform JavaParser ASTs.
 */
public final class Mappers {

  private Mappers() {}

  public static VariableDeclarator toFirstVariableDecorator(final ExpressionStmt stmt) {
    VariableDeclarationExpr vd = (VariableDeclarationExpr) stmt.getExpression();
    return vd.getVariable(0);
  }
}
