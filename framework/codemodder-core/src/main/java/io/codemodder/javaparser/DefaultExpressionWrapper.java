package io.codemodder.javaparser;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.ast.ASTTransforms.addStaticImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import java.util.Optional;

final class DefaultExpressionWrapper implements JavaParserTransformer.ExpressionWrapper {
  private final Expression expression;

  DefaultExpressionWrapper(final Expression expression) {
    this.expression = expression;
  }

  @Override
  public boolean withStaticMethod(
      final String className, final String methodName, final boolean isStaticImport) {
    Optional<CompilationUnit> cuOpt = expression.findAncestor(CompilationUnit.class);
    if (cuOpt.isEmpty()) {
      return false;
    }
    CompilationUnit cu = cuOpt.get();
    Node parent = expression.getParentNode().get();
    String simpleName = className.substring(className.lastIndexOf('.') + 1);
    MethodCallExpr safeCall =
        isStaticImport
            ? new MethodCallExpr(methodName, expression)
            : new MethodCallExpr(
                new NameExpr(simpleName), methodName, NodeList.nodeList(expression));
    parent.replace(expression, safeCall);
    if (isStaticImport) {
      addStaticImportIfMissing(cu, className + "." + methodName);
    } else {
      addImportIfMissing(cu, className);
    }
    return true;
  }
}
