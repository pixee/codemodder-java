package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;

/**
 * This visitor prevents {@link Runtime} OS / command injection by wrapping the execute calls with a
 * semantic analysis wrapper ensuring certain requirements.
 */
public final class RuntimeExecVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new RuntimeExecVisitor(cu);
  }

    @Override
    public String ruleId() {
        return hardenRuntimeExecRuleId;
    }

    private static class RuntimeExecVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private RuntimeExecVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr n, final FileWeavingContext context) {
      if ("exec".equals(n.getNameAsString())
          && n.getScope().isPresent()
          && context.isLineIncluded(n)) {
        final TypeLocator resolver = TypeLocator.createDefault(cu);
        final Expression scope = n.getScope().get();
        final String typeName = resolver.locateType(scope);
        if ("java.lang.Runtime".equals(typeName) || isRuntimeGetRuntime(scope)) {
          // need to replace:
          //   Runtime.exec(cmd, ...)
          //   io.pixee.security.SystemCommand.runCommand(Runtime.getRuntime(), cmd, ...)
          NameExpr callbackClass = new NameExpr(io.pixee.security.SystemCommand.class.getName());
          MethodCallExpr safeExpression = new MethodCallExpr(callbackClass, "runCommand");
          NodeList<Expression> nodeList = new NodeList<>();
          nodeList.add(scope);
          nodeList.addAll(n.getArguments());
          safeExpression.setArguments(nodeList);
          context.addWeave(Weave.from(n.getRange().get().begin.line, hardenRuntimeExecRuleId));
          return super.visit(safeExpression, context);
        }
      }
      return super.visit(n, context);
    }

    private boolean isRuntimeGetRuntime(final Expression originalScope) {
      if (originalScope.isMethodCallExpr()) {
        MethodCallExpr methodCallExpr = originalScope.asMethodCallExpr();
        if (methodCallExpr.getScope().isPresent()) {
          Expression scope = methodCallExpr.getScope().get();
          return scope.toString().equals("Runtime")
              && methodCallExpr.getNameAsString().equals("getRuntime");
        }
      }
      return false;
    }
  }

  private static final String hardenRuntimeExecRuleId = "pixee:java/harden-runtime-exec";
}
