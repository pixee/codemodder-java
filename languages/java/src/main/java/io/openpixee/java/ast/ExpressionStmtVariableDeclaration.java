package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

public final class ExpressionStmtVariableDeclaration extends LocalVariableDeclaration {

  private final ExpressionStmt stmt;

  public ExpressionStmtVariableDeclaration(
      ExpressionStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = stmt;
    this.vde = vde;
    this.vd = vd;
    this.scope = null;
  }

  @Override
  public ExpressionStmt getStatement() {
    return stmt;
  }

  @Override
  public LocalVariableScope findScope() {
    return LocalVariableScope.fromLocalDeclaration(stmt, vd);
  }
}
