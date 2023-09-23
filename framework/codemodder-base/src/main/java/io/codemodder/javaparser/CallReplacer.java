package io.codemodder.javaparser;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;

public interface CallReplacer {
  /**
   * Performs the actual transformation of replacing the given expression a static method call.
   *
   * @param className the class name of the static method
   * @param methodName the method name
   * @return a builder for the replacement
   */
  CallReplacerBuilder withStaticMethod(String className, String methodName);

  /**
   * Performs the actual transformation of replacing the given expression another arbitrary
   * expression.
   *
   * @param expression the expression to replace with
   */
  void withExpression(Expression expression);

  /** Builder for {@link CallReplacer}. */
  interface CallReplacerBuilder {

    /** Use a static import. */
    CallReplacerBuilder withStaticImport();

    /** Replace with a static method call with new arguments. */
    boolean withNewArguments(NodeList<Expression> newArguments);

    /** Replace with a static method call with the same arguments. */
    boolean withSameArguments();
  }
}
