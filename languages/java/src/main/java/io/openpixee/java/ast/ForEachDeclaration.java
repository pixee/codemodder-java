package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;

public final class ForEachDeclaration extends LocalVariableDeclaration {

  private final ForEachStmt stmt;

  public ForEachDeclaration(ForEachStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = stmt;
    this.vde = vde;
    this.vd = vd;
    this.scope = null;
  }

  @Override
  public ForEachStmt getStatement() {
    return stmt;
  }

  public LocalVariableScope findScope() {
    return LocalVariableScope.fromForEachDeclaration(stmt);
  }
}
