package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.security.*;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;

public final class UnsafeReadlineVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    return new UnsafeReadlineVisitor(cu);
  }

    @Override
    public String ruleId() {
        return readlineRuleId;
    }

    private static class UnsafeReadlineVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private UnsafeReadlineVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(MethodCallExpr n, final FileWeavingContext context) {
      if ("readLine".equals(n.getNameAsString())
          && n.getScope().isPresent()
          && context.isLineIncluded(n)) {
        NodeList<Expression> arguments = n.getArguments();
        if (arguments.size() == 0) {
          Expression readerScope = n.getScope().get();
          TypeLocator resolver = TypeLocator.createDefault(cu);
          if ("java.io.BufferedReader".equals(resolver.locateType(readerScope))) {
            MethodCallExpr safeExpression =
                new MethodCallExpr(new NameExpr(SafeIO.class.getName()), "boundedReadLine");
            safeExpression.setArguments(
                NodeList.nodeList(readerScope, new IntegerLiteralExpr(defaultLineMaximum)));
            context.addWeave(Weave.from(n.getRange().get().begin.line, readlineRuleId));
            n.getParentNode().get().replace(n, safeExpression);
            n = safeExpression;
          }
        }
      }
      return super.visit(n, context);
    }
  }

  private static final int defaultLineMaximum = 1_000_000; // 1 MB
  private static final String readlineRuleId = "pixee:java/readline";
}
