package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.openpixee.java.DependencyGAV;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.Weave;
import io.openpixee.security.Jakarta;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Makes sure that internal Jakarta forwards don't go to places they shouldn't (e.g.,
 * /WEB-INF/web.xml.)
 */
public final class JakartaForwardVisitoryFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("getRequestDispatcher"),
            MethodCallPredicateFactory.withArgumentCount(1),
            MethodCallPredicateFactory.withArgumentCodeContains(0, "validate").negate(),
            MethodCallPredicateFactory.withArgumentNodeType(0, StringLiteralExpr.class).negate(),
            MethodCallPredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate());

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            ASTs.addImportIfMissing(cu, Jakarta.class);
            MethodCallExpr safeExpression =
                new MethodCallExpr(
                    new NameExpr(Jakarta.class.getSimpleName()), "validateForwardPath");
            safeExpression.setArguments(NodeList.nodeList(methodCallExpr.getArgument(0)));
            methodCallExpr.setArgument(0, safeExpression);
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    pathCheckingRuleId,
                    DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return pathCheckingRuleId;
  }

  private static final String pathCheckingRuleId = "pixee:java/validate-jakarta-forward-path";
}
