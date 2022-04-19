package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Random;

/**
 * This visitor replaces constant parameters to {@link Random#setSeed(long)} with something more unpredictable.
 */
public final class PredictableSeedVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new PredictableSeedVisitor(cu);
  }

    @Override
    public String ruleId() {
        return unpredictableSeedRuleId;
    }

    private static class PredictableSeedVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private PredictableSeedVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final ExpressionStmt n, final FileWeavingContext context) {
      Expression expression = n.getExpression();
      if (expression.isMethodCallExpr()) {
        MethodCallExpr methodCallExpr = expression.asMethodCallExpr();
        if ("setSeed".equals(methodCallExpr.getName().asString()) && context.isLineIncluded(n)) {
          NodeList<Expression> arguments = methodCallExpr.getArguments();
          if (arguments.size() == 1) {
            if (methodCallExpr.getScope().isPresent()) {
              TypeLocator resolver = TypeLocator.createDefault(cu);
              String typeName = resolver.locateType(methodCallExpr.getScope().get());
              if (Random.class.getName().equals(typeName)
                  || SecureRandom.class.getName().equals(typeName)) {
                Expression seedExpression = arguments.get(0);
                if (seedExpression.isLiteralExpr()) {
                  MethodCallExpr safeExpression =
                      new MethodCallExpr(new NameExpr(System.class.getName()), "currentTimeMillis");
                  arguments.set(0, safeExpression);
                  context.addWeave(
                      Weave.from(n.getRange().get().begin.line, unpredictableSeedRuleId));
                }
              }
            }
          }
        }
      }
      return super.visit(n, context);
    }
  }

  private static final String unpredictableSeedRuleId = "pixee:java/unpredictable-seed";
}
