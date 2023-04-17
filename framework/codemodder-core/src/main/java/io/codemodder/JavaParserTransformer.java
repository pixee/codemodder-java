package io.codemodder;

import com.github.javaparser.ast.expr.Expression;

/** A utility for making it easy to transform JavaParser ASTs. */
public abstract class JavaParserTransformer {

  private JavaParserTransformer() {}

  /** Useful for wrapping an expression with a surrounding method call. */
  public static JavaParserExpressionWrapper wrapExpression(Expression expression) {
    return new DefaultJavaParserExpressionWrapper(expression);
  }

  public interface JavaParserExpressionWrapper {
    /**
     * Performs the actual transformation of wrapping the given expression with the given static
     * method.
     *
     * @param className the class name of the static method
     * @param methodName the method name
     * @return true if the transformation was successful, false otherwise
     */
    boolean withStaticMethod(String className, String methodName);
  }
}
