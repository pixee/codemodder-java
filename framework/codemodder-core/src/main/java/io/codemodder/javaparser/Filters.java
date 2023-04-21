package io.codemodder.javaparser;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;

/**
 * A utility for making it easy to filter JavaParser ASTs, intended to make things easier to build
 * {@link ASTExpectations} queries.
 */
final class Filters {

  private Filters() {}

  /** Returns true if the variable passed in is referenced exactly the number of times passed in. */
  static boolean isVariableReferencedExactly(final VariableDeclarator vd, final int times) {
    long count =
        vd.findAncestor(MethodDeclaration.class).get().findAll(NameExpr.class).stream()
            .filter(n -> n.getNameAsString().equals(vd.getNameAsString()))
            .count();
    return count == times;
  }
}
