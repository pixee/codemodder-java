package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Objects;

/**
 * Holds the nodes in the AST that represents a variable declaration as a try resource. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-14.html#jls-14.20.3">Java Language
 * Specification - Section 14.20.3</a> for more details.
 */
public final class TryResourceDeclaration extends LocalVariableDeclaration {

  private final TryStmt stmt;

  public TryResourceDeclaration(
      final TryStmt stmt, final VariableDeclarationExpr vde, final VariableDeclarator vd) {
    this.stmt = Objects.requireNonNull(stmt);
    this.vde = Objects.requireNonNull(vde);
    this.vd = Objects.requireNonNull(vd);
    this.scope = null;
  }

  /** Returns the {@link TryStmt} {@link Node} that holds the resource declaration. */
  @Override
  public TryStmt getStatement() {
    return stmt;
  }

  @Override
  protected LocalVariableScope findScope() {
    return LocalVariableScope.fromTryResource(stmt, vd);
  }
}
