package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;

/**
 * Makes sure that internal Jakarta forwards don't go to places they shouldn't (e.g., /WEB-INF/web.xml.)
 */
public final class JakartaForwardVisitoryFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    return new JakartaForwardVisitor(cu);
  }

    @Override
    public String ruleId() {
        return pathCheckingRuleId;
    }

    private static class JakartaForwardVisitor extends ModifierVisitor<FileWeavingContext> {

    private final CompilationUnit cu;

    private JakartaForwardVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr n, final FileWeavingContext context) {
      if ("getRequestDispatcher".equals(n.getNameAsString()) && context.isLineIncluded(n)) {
        NodeList<Expression> arguments = n.getArguments();
        if (arguments.size() == 1) {
          Expression expression = arguments.get(0);
          if (!expression.isLiteralExpr() && isntAlreadySanitized(expression)) {
            MethodCallExpr safeExpression =
                new MethodCallExpr(new NameExpr(io.pixee.security.Jakarta.class.getName()), "validateForwardPath");
            safeExpression.setArguments(NodeList.nodeList(expression));
            arguments.set(0, safeExpression);
            context.addWeave(
                Weave.from(expression.getRange().get().begin.line, pathCheckingRuleId));
          }
        }
      }
      return super.visit(n, context);
    }

    // TODO: needs much improved
    private boolean isntAlreadySanitized(final Expression expression) {
      return !expression.toString().contains("validateForwardPath");
    }
  }

  private static final String pathCheckingRuleId = "pixee:java/validate-jakarta-forward-path";
}
