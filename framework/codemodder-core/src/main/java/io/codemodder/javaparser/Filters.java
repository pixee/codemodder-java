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

  /**
   * Returns true if the variable passed in is referenced exactly the number of times passed in.
   * This method can return wrong values in the case where multiple variables with the same name are
   * available in the same scope.
   *
   * <p>Consider this code where the same variable `a` is declared twice in the same scope: <code>
   * class A{
   *   int a = 0;
   *   void foo(){
   *     a = 1;
   *     int a = 2;
   *     System.out.println(a);
   *   }
   * }
   * </code>
   */
  static boolean isVariableProbablyReferencedExactly(final VariableDeclarator vd, final int times) {
    long count =
        vd.findAncestor(MethodDeclaration.class).stream()
            .flatMap(md -> md.findAll(NameExpr.class).stream())
            .filter(n -> n.getNameAsString().equals(vd.getNameAsString()))
            .count();
    return count == times;
  }
}
