package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForStmt;

public final class ForInitDeclaration extends LocalVariableDeclaration {

  private ForStmt stmt;

  public ForInitDeclaration(ForStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = stmt;
    this.vde = vde;
    this.vd = vd;
    this.scope = null;
  }

  @Override
  public ForStmt getStatement() {
    return stmt;
  }

  public LocalVariableScope findScope() {
    return LocalVariableScope.fromForDeclaration(stmt, vd);
  }
}
