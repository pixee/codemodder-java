package io.codemodder.javaparser;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.ast.ASTTransforms.addStaticImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import java.util.Objects;
import java.util.Optional;

final class DefaultCallReplacer implements CallReplacer {

  private final Expression call;
  private final NodeList<Expression> arguments;

  DefaultCallReplacer(final Expression expression) {
    if (!(expression instanceof NodeWithArguments)) {
      throw new IllegalArgumentException("Expression must be a NodeWithArguments");
    }
    this.call = Objects.requireNonNull(expression);
    this.arguments = ((NodeWithArguments<Expression>) expression).getArguments();
  }

  @Override
  public CallReplacerBuilder withStaticMethod(final String className, final String methodName) {
    return new DefaultCallReplacerBuilder(className, methodName);
  }

  @Override
  public void withExpression(final Expression expression) {
    Node parent = call.getParentNode().get();
    parent.replace(call, expression);
  }

  private class DefaultCallReplacerBuilder implements CallReplacerBuilder {

    private final String className;
    private final String methodName;
    private boolean useStaticImport;

    private DefaultCallReplacerBuilder(final String className, final String methodName) {
      this.className = Objects.requireNonNull(className);
      this.methodName = Objects.requireNonNull(methodName);
      this.useStaticImport = false;
    }

    @Override
    public CallReplacerBuilder withStaticImport() {
      this.useStaticImport = true;
      return this;
    }

    @Override
    public boolean withNewArguments(final NodeList<Expression> newArguments) {
      return transform(className, methodName, useStaticImport, newArguments);
    }

    @Override
    public boolean withSameArguments() {
      return transform(className, methodName, useStaticImport, arguments);
    }

    private boolean transform(
        final String className,
        final String methodName,
        final boolean isStaticImport,
        final NodeList<Expression> newArguments) {
      Optional<CompilationUnit> cu = call.findCompilationUnit();
      if (cu.isEmpty()) {
        return false;
      }
      Node parent = call.getParentNode().get();
      String simpleName = className.substring(className.lastIndexOf('.') + 1);
      MethodCallExpr safeCall =
          isStaticImport
              ? new MethodCallExpr(methodName, newArguments.toArray(new Expression[0]))
              : new MethodCallExpr(new NameExpr(simpleName), methodName, newArguments);
      parent.replace(call, safeCall);
      if (isStaticImport) {
        addStaticImportIfMissing(cu.get(), className + "." + methodName);
      } else {
        addImportIfMissing(cu.get(), className);
      }
      return true;
    }
  }
}
