package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.NodePredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.VisitorFactoryNg;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This visitor prevents {@link Runtime} OS / command injection by wrapping the execute calls with a
 * semantic analysis wrapper ensuring certain requirements.
 */
public final class RuntimeExecVisitorFactoryNg implements VisitorFactoryNg {

  @Override
  public ModifierVisitor<FileWeavingContext> createVisitor(
      final File file, final CompilationUnit cu) {
    Set<Predicate<MethodCallExpr>> predicates = Set.of(
            NodePredicateFactory.withMethodName("exec"),
            NodePredicateFactory.withArgumentCount(0).negate(),
            NodePredicateFactory.withScopeType(cu, "java.lang.Runtime").or(new Predicate<MethodCallExpr>() {
              @Override
              public boolean test(final MethodCallExpr execCall) {
                Expression originalScope = execCall.getScope().get();
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
            })
    );

    // need to replace:
    //   Runtime.exec(cmd, ...)
    //   io.pixee.security.SystemCommand.runCommand(Runtime.getRuntime(), cmd, ...)
    Transformer<MethodCallExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<MethodCallExpr> transform(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
        Expression scope = methodCallExpr.getScope().get();
        NameExpr callbackClass = new NameExpr(io.pixee.security.SystemCommand.class.getName());
        MethodCallExpr safeExpression = new MethodCallExpr(callbackClass, "runCommand");
        NodeList<Expression> nodeList = new NodeList<>();
        nodeList.add(scope);
        nodeList.addAll(methodCallExpr.getArguments());
        safeExpression.setArguments(nodeList);
        Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, hardenRuntimeExecRuleId);
        return new TransformationResult<>(Optional.of(safeExpression), weave);
      }
    };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
      return hardenRuntimeExecRuleId;
  }

  private static final String hardenRuntimeExecRuleId = "pixee:java/harden-runtime-exec";
}
