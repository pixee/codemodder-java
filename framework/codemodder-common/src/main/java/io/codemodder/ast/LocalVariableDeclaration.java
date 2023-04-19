package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;

/**
 * Holds the nodes in the AST that represents several types of local declaration of a variable. See
 * <a href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.1">Java Language
 * Specification - Section 6.1</a> for all the possible ways a local variable can be declarared.
 */
public abstract class LocalVariableDeclaration {

  protected VariableDeclarationExpr vde;
  protected VariableDeclarator vd;
  protected LocalVariableScope scope;

  /** Returns the name of the local variable in this declaration as a {@link String}. */
  public String getName() {
    return vd.getNameAsString();
  }

  /** Returns the {@link LocalVariableScope} of the local variable in this declaration. */
  public LocalVariableScope getScope() {
    if (scope == null) scope = findScope();
    return scope;
  }

  /** Returns the {@link VariableDeclarator} {@link Node} that holds this local declaration. */
  public VariableDeclarator getVariableDeclarator() {
    return vd;
  }

  /** Returns the {@link VariableDeclarationExpr} {@link Node} that holds this local declaration. */
  public VariableDeclarationExpr getVariableDeclarationExpr() {
    return vde;
  }

  /** Returns the {@link Statement} {@link Node} that holds this local declaration. */
  public abstract Statement getStatement();

  protected abstract LocalVariableScope findScope();

  @Override
  public String toString() {
    return getStatement().toString();
  }

  /**
   * For a given {@link VariableDeclarator} {@code vd}, check if it is part of a local declaration
   * and return its associated {@link LocalVariableDeclaration} object, if applicable.
   */
  public static Optional<LocalVariableDeclaration> fromVariableDeclarator(
      final VariableDeclarator vd) {
    var maybeVDE =
        vd.getParentNode()
            .map(p -> p instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) p : null);
    if (maybeVDE.isEmpty()) return Optional.empty();
    var vde = maybeVDE.get();
    var stmt = (Statement) vde.getParentNode().get();
    if (stmt instanceof TryStmt)
      return Optional.of(new TryResourceDeclaration(stmt.asTryStmt(), vde, vd));
    if (stmt instanceof ExpressionStmt)
      return Optional.of(new ExpressionStmtVariableDeclaration(stmt.asExpressionStmt(), vde, vd));
    if (stmt instanceof ForEachStmt)
      return Optional.of(new ForEachDeclaration(stmt.asForEachStmt(), vde, vd));
    if (stmt instanceof ForStmt)
      return Optional.of(new ForInitDeclaration(stmt.asForStmt(), vde, vd));
    // Still lacking the case for PatternExpr since it's not supported by JavaParser
    return Optional.empty();
  }
}
