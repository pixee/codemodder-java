package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.security.*;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class UnsafeReadlineVisitorFactory implements VisitorFactory {


  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates = List.of(
            MethodCallPredicateFactory.withName("readLine"),
            MethodCallPredicateFactory.withArgumentCount(0),
            MethodCallPredicateFactory.withScopeType(cu, "java.io.BufferedReader"),
            MethodCallPredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate()
    );

    Transformer<MethodCallExpr,MethodCallExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<MethodCallExpr> transform(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
        Expression readerScope = methodCallExpr.getScope().get();
        MethodCallExpr safeExpression =
                new MethodCallExpr(new NameExpr(SafeIO.class.getName()), "boundedReadLine");
        safeExpression.setArguments(
                NodeList.nodeList(readerScope, new IntegerLiteralExpr(defaultLineMaximum)));
        Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, readlineRuleId);
        methodCallExpr.getParentNode().get().replace(methodCallExpr, safeExpression);
        return new TransformationResult<>(Optional.of(safeExpression), weave);
      }
    };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

    @Override
    public String ruleId() {
        return readlineRuleId;
    }

  private static final int defaultLineMaximum = 1_000_000; // 1 MB
  private static final String readlineRuleId = "pixee:java/readline";
}
