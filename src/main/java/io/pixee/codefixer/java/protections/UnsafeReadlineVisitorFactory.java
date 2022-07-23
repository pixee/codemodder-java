package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.DependencyGAV;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.security.*;
import java.io.BufferedReader;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Prevents attacks whereby attackers can provide infinite data to a {@link
 * BufferedReader#readLine()} consumer until it runs out of memory.
 */
public final class UnsafeReadlineVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("readLine"),
            MethodCallPredicateFactory.withArgumentCount(0),
            MethodCallPredicateFactory.withScopeType(cu, "java.io.BufferedReader"),
            MethodCallPredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate());

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            Expression readerScope = methodCallExpr.getScope().get();
            MethodCallExpr safeExpression =
                new MethodCallExpr(new NameExpr(BoundedLineReader.class.getName()), "readLine");
            safeExpression.setArguments(
                NodeList.nodeList(readerScope, new IntegerLiteralExpr(defaultLineMaximum)));
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    readlineRuleId,
                    DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
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
  private static final String readlineRuleId = "pixee:java/limit-readline";
}
