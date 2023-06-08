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

final class DefaultCallReplacer implements JavaParserTransformer.CallReplacer {

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
  public boolean withStaticMethodWithSameArguments(
      final String className, final String methodName, final boolean isStaticImport) {
    Optional<CompilationUnit> cu = call.findCompilationUnit();
    if (cu.isEmpty()) {
      return false;
    }
    Node parent = call.getParentNode().get();
    String simpleName = className.substring(className.lastIndexOf('.') + 1);
    MethodCallExpr safeCall =
        isStaticImport
            ? new MethodCallExpr(methodName, arguments.toArray(new Expression[0]))
            : new MethodCallExpr(new NameExpr(simpleName), methodName, arguments);
    parent.replace(call, safeCall);
    if (isStaticImport) {
      addStaticImportIfMissing(cu.get(), className + "." + methodName);
    } else {
      addImportIfMissing(cu.get(), className);
    }
    return true;
  }
}
