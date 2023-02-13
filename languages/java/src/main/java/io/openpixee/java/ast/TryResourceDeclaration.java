package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.TryStmt;

public final class TryResourceDeclaration extends LocalVariableDeclaration {

  private final TryStmt stmt;

  public TryResourceDeclaration(TryStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = stmt;
    this.vde = vde;
    this.vd = vd;
    this.scope = null;
  }

  @Override
  public TryStmt getStatement() {
    return stmt;
  }

  public LocalVariableScope findScope() {
    return LocalVariableScope.fromTryResource(stmt, vd);
  }
}
