package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import java.util.Objects;

/**
 * Holds the nodes in the AST that represents a variable declaration in an enhanced for statement .
 * See <a href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-14.html#jls-14.14">Java
 * Language Specification - Section 14.14.14</a> for more details.
 */
public final class ForEachDeclaration extends LocalVariableDeclaration {

  private final ForEachStmt stmt;

  public ForEachDeclaration(ForEachStmt stmt, VariableDeclarationExpr vde, VariableDeclarator vd) {
    this.stmt = Objects.requireNonNull(stmt);
    this.vde = Objects.requireNonNull(vde);
    this.vd = Objects.requireNonNull(vd);
    this.scope = null;
  }

  /** Returns the {@link ForEachStmt} {@link Node} that holds the declaration. */
  @Override
  public ForEachStmt getStatement() {
    return stmt;
  }

  @Override
  protected LocalScope findScope() {
    return LocalScope.fromForEachDeclaration(stmt);
  }
}
