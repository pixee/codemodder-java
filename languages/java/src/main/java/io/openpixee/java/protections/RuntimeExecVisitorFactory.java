package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.DependencyGAV;
import io.codemodder.Weave;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.security.SystemCommand;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This visitor prevents {@link Runtime} OS / command injection by wrapping the execute calls with a
 * semantic analysis wrapper ensuring certain requirements.
 */
public final class RuntimeExecVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("exec"),
            MethodCallPredicateFactory.withArgumentCount(0).negate(),
            MethodCallPredicateFactory.withScopeType(cu, "java.lang.Runtime")
                .or(
                    new Predicate<MethodCallExpr>() {
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
                    }));

    // need to replace:
    //   Runtime.exec(cmd, ...)
    //   io.openpixee.security.SystemCommand.runCommand(Runtime.getRuntime(), cmd, ...)
    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            Expression scope = methodCallExpr.getScope().get();
            ASTTransforms.addImportIfMissing(cu, SystemCommand.class);
            NameExpr callbackClass =
                new NameExpr(io.openpixee.security.SystemCommand.class.getSimpleName());
            MethodCallExpr safeExpression = new MethodCallExpr(callbackClass, "runCommand");
            NodeList<Expression> nodeList = new NodeList<>();
            nodeList.add(scope);
            nodeList.addAll(methodCallExpr.getArguments());
            safeExpression.setArguments(nodeList);
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    hardenRuntimeExecRuleId,
                    DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
            return new TransformationResult<>(Optional.of(safeExpression), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return hardenRuntimeExecRuleId;
  }

  private static final String hardenRuntimeExecRuleId = "pixee:java/harden-process-creation";
}
