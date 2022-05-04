package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.TransformationException;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.security.Deserialization;
import java.io.File;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This visitor prevents deserialization attacks by injecting an {@link java.io.ObjectInputFilter}
 * that blacklists known gadget types.
 */
public final class DeserializationVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("readObject"),
            MethodCallPredicateFactory.withArgumentCount(0),
            MethodCallPredicateFactory.withScopeType(cu, "java.io.ObjectInputStream"),
            MethodCallPredicateFactory.withMethodPreviouslyCalledOnScope("setObjectInputFilter")
                .negate(),
            MethodCallPredicateFactory.withMethodPreviouslyCalledOnScope(
                    "enableObjectFilterIfUnprotected")
                .negate(),
            new OriginalDeserializationLogicPredicate());

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context)
              throws TransformationException {
            Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(methodCallExpr);
            if (methodRef.isEmpty()) {
              throw new TransformationException(
                  "shouldn't be transforming deserialization outside method bodies");
            }
            MethodDeclaration method = methodRef.get();
            Expression scope = methodCallExpr.getScope().get();
            Node parentNode = methodCallExpr.getParentNode().get();
            boolean wasDeclaredOutsideScope =
                scope.isNameExpr()
                    && method.findAll(VariableDeclarator.class).stream()
                        .filter(this::isNormalObjectInputStreamConstructor)
                        .noneMatch(var -> Objects.equals(scope.toString(), var.getNameAsString()));

            if (parentNode instanceof Statement) {
              injectHardenFilterBeforeBareStatement(
                  context, methodCallExpr, scope, (Statement) parentNode);
            } else if (parentNode instanceof VariableDeclarator) {
              if (wasDeclaredOutsideScope) {
                /*
                 * This may be a safe subclass of ObjectInputStream -- we can't confirm that, so the best we can
                 * do is inject the hardening filter.
                 */
                injectHardenFilterBeforeVariableAssigned(
                    context, methodCallExpr, scope, (VariableDeclarator) parentNode);
              } else {
                /*
                 * Since we know this is a normal ObjectInputStream, and not a safe subclass, we have the choice
                 * of either:
                 *
                 * 1. Replace the ObjectInputStream with a call like: io.pixee.security.Deserialization.createSafeObjectInputStream(is)
                 * 2. Inject an ObjectInputFilter.
                 */
                injectHardenFilterBeforeVariableAssigned(
                    context, methodCallExpr, scope, (VariableDeclarator) parentNode);
              }
            } else {
              throw new TransformationException("unknown expression holder: " + parentNode);
            }

            Weave weave =
                Weave.from(methodCallExpr.getRange().get().begin.line, deserializationRuleId);
            return new TransformationResult<>(Optional.empty(), weave);
          }

          /**
           * This is for naked calls that don't even assign the deserialized object like:
           *
           * <p>ois.readObject();
           *
           * <p>This is obviously not super realistic, but we should have it in case people are
           * testing our stuff in a PoC kind of situation.
           */
          private void injectHardenFilterBeforeBareStatement(
              final FileWeavingContext context,
              final MethodCallExpr readObjectInvocation,
              final Expression originalScope,
              final Statement vulnerableStatement) {
            var hardeningStmt = generateFilterHardeningStatement(originalScope);
            ASTs.addStatementBeforeStatement(vulnerableStatement, hardeningStmt);
          }

          /**
           * This is for assignment expressions like: ObjectInputStream ois = ...; Object o =
           * ois.readObject();
           */
          private void injectHardenFilterBeforeVariableAssigned(
              final FileWeavingContext context,
              final MethodCallExpr readObjectInvocation,
              final Expression originalScope,
              final VariableDeclarator variableDeclaration) {
            var variableDeclarationExpression =
                (VariableDeclarationExpr) variableDeclaration.getParentNode().get();
            var expressionStmt =
                (ExpressionStmt) variableDeclarationExpression.getParentNode().get();
            var hardeningStmt = generateFilterHardeningStatement(originalScope);
            ASTs.addStatementBeforeStatement(expressionStmt, hardeningStmt);
          }

          private boolean isNormalObjectInputStreamConstructor(
              final VariableDeclarator variableDeclarator) {
            var type = variableDeclarator.getType();
            if (!type.isClassOrInterfaceType()) {
              return false;
            }
            return type.asClassOrInterfaceType().getNameAsString().equals("ObjectInputStream");
          }

          /**
           * Generates an expression to invoke {@link
           * io.pixee.security.Deserialization#enableObjectFilterIfUnprotected(ObjectInputStream)}
           * on the original scope (the {@link ObjectInputStream}).
           */
          private Statement generateFilterHardeningStatement(final Expression originalScope) {
            // this statement is the callback to our hardening code
            var callbackClass = new NameExpr(Deserialization.class.getName());
            var hardenStatement =
                new MethodCallExpr(callbackClass, "enableObjectFilterIfUnprotected");
            hardenStatement.addArgument(originalScope);
            return new ExpressionStmt(hardenStatement);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  private static class OriginalDeserializationLogicPredicate implements Predicate<MethodCallExpr> {
    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(methodCallExpr);
      if (methodRef.isEmpty()) {
        return false;
      }
      MethodDeclaration method = methodRef.get();
      Expression scope = methodCallExpr.getScope().get();
      var setObjectInputFilterCallsOnScope =
          method.findAll(MethodCallExpr.class).stream()
              .filter(
                  expr ->
                      expr.getScope().isPresent() && Objects.equals(expr.getScope().get(), scope))
              .filter(expr -> "setObjectInputFilter".equals(expr.getNameAsString()))
              .takeWhile(expr -> expr != methodCallExpr)
              .collect(Collectors.toList());

      if (!setObjectInputFilterCallsOnScope.isEmpty()) {
        return false;
      }

      var hardeningStatementsOnScope =
          method.findAll(MethodCallExpr.class).stream()
              .filter(expr -> hasFirstArgumentEqualingScope(expr, scope))
              .filter(expr -> "enableObjectFilterIfUnprotected".equals(expr.getNameAsString()))
              .takeWhile(expr -> expr != methodCallExpr)
              .collect(Collectors.toList());

      return hardeningStatementsOnScope.isEmpty();
    }

    private boolean hasFirstArgumentEqualingScope(
        final MethodCallExpr expr, final Expression originalScope) {
      var arguments = expr.getArguments();
      if (arguments.size() == 0) {
        return false;
      }
      return arguments.get(0).equals(originalScope);
    }
  }

  @Override
  public String ruleId() {
    return deserializationRuleId;
  }

  private static final String deserializationRuleId = "pixee:java/deserialization-java-readObject";
}
