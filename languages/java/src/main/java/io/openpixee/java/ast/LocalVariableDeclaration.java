package io.openpixee.java.ast;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Holds the nodes in the AST that represents a local declaration of a variable. See
 * <a href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.1">Java Language Specification - Section 6.1</a>
 */
public abstract class LocalVariableDeclaration {

  VariableDeclarationExpr vde;
  VariableDeclarator vd;
  LocalVariableScope scope;

  public String getName() {
    return vd.getNameAsString();
  }

  public LocalVariableScope getScope() {
    if (scope == null) scope = findScope();
    return scope;
  }

  public VariableDeclarator getVariableDeclarator() {
    return vd;
  }

  public VariableDeclarationExpr getVariableDeclarationExpr() {
    return vde;
  }

  public abstract Statement getStatement();

  protected abstract LocalVariableScope findScope();
}
