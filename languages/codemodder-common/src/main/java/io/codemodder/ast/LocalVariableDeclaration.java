package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.Statement;

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
}
