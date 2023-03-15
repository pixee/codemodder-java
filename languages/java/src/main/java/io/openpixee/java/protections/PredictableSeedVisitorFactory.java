package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * This visitor replaces constant parameters to {@link Random#setSeed(long)} with something more
 * unpredictable.
 */
public final class PredictableSeedVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("setSeed"),
            MethodCallPredicateFactory.withArgumentCount(1),
            MethodCallPredicateFactory.withScopeType(cu, "java.util.Random")
                .or(MethodCallPredicateFactory.withScopeType(cu, "java.security.SecureRandom")),
            MethodCallPredicateFactory.withArgumentNodeType(0, LiteralExpr.class));

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            MethodCallExpr safeExpression =
                new MethodCallExpr(new NameExpr(System.class.getName()), "currentTimeMillis");
            NodeList<Expression> arguments = methodCallExpr.getArguments();
            arguments.set(0, safeExpression);
            Weave weave =
                Weave.from(methodCallExpr.getRange().get().begin.line, unpredictableSeedRuleId);
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return unpredictableSeedRuleId;
  }

  private static final String unpredictableSeedRuleId = "pixee:java/make-prng-seed-unpredictable";
}
