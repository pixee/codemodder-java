package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.Objects;

/**
 * Holds the nodes in the AST that represents a local variable declaration statement. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-14.html#jls-14.4.2">Java Language
 * Specification - Section 14.4.2</a> for more details.
 */
public final class ExpressionStmtVariableDeclaration extends LocalVariableDeclaration {

  private final ExpressionStmt stmt;

  public ExpressionStmtVariableDeclaration(
      final ExpressionStmt stmt, final VariableDeclarationExpr vde, final VariableDeclarator vd) {
    this.stmt = Objects.requireNonNull(stmt);
    this.vde = Objects.requireNonNull(vde);
    this.vd = Objects.requireNonNull(vd);
    this.scope = null;
  }

  /** Returns the {@link ExpressionStmt} {@link Node} that holds this local declaration. */
  @Override
  public ExpressionStmt getStatement() {
    return stmt;
  }

  @Override
  protected LocalVariableScope findScope() {
    return LocalVariableScope.fromLocalDeclaration(stmt, vd);
  }
}
