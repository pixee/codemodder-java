package io.codemodder.javaparser;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/** A utility for making it easy to transform JavaParser ASTs. */
public abstract class JavaParserTransformer {

  private JavaParserTransformer() {}

  /** Useful for wrapping an expression with a surrounding method call. */
  public static ExpressionWrapper wrap(final Expression expression) {
    return new DefaultExpressionWrapper(expression);
  }

  /**
   * Useful for replacing an expression (method call or object creation) with a surrounding method
   * call.
   */
  public static CallReplacer replace(final Expression expression) {
    return new DefaultCallReplacer(expression);
  }

  /** Creates a new array creation expression. */
  public static ArrayCreationExpr newArray(final String type, final Expression... values) {
    final ArrayCreationExpr array = new ArrayCreationExpr(new ClassOrInterfaceType(type));
    array.setLevels(NodeList.nodeList(new ArrayCreationLevel()));
    array.setInitializer(new ArrayInitializerExpr(NodeList.nodeList(values)));
    return array;
  }

  public interface ExpressionWrapper {
    /**
     * Performs the actual transformation of wrapping the given expression with the given static
     * method.
     *
     * @param className the class name of the static method
     * @param methodName the method name
     * @param isStaticImport whether the static method should be imported and referenced in an
     *     unqualified way
     * @return true if the transformation was successful, false otherwise
     */
    boolean withStaticMethod(String className, String methodName, boolean isStaticImport);
  }
}
