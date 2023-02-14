package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import java.util.Objects;

/**
 * Holds the nodes in the AST that represents a variable declaration in a for statement init
 * expression. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-14.html#jls-14.14">Java Language
 * Specification - Section 14.14.14</a> for more details.
 */
public final class ForInitDeclaration extends LocalVariableDeclaration {

  private final ForStmt stmt;

  public ForInitDeclaration(ForStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = Objects.requireNonNull(stmt);
    this.vde = Objects.requireNonNull(vde);
    this.vd = Objects.requireNonNull(vd);
    this.scope = null;
  }

  /** Returns the {@link ForStmt} {@link Node} that holds the declaration. */
  @Override
  public ForStmt getStatement() {
    return stmt;
  }

  @Override
  protected LocalVariableScope findScope() {
    return LocalVariableScope.fromForDeclaration(stmt, vd);
  }
}
