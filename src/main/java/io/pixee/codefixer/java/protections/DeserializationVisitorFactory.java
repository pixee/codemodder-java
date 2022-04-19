package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.security.*;
import java.io.File;
import java.io.ObjectInputStream;
import java.util.Objects;
import java.util.stream.Collectors;

import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This visitor prevents deserialization attacks by injecting an {@link java.io.ObjectInputFilter}
 * that blacklists known gadget types.
 */
public final class DeserializationVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new DeserializationVisitor(cu);
  }

    @Override
    public String ruleId() {
        return deserializationRuleId;
    }

    static class DeserializationVisitor extends ModifierVisitor<FileWeavingContext> {

    private final TypeLocator resolver;
    private final NameExpr callbackClass;

    private DeserializationVisitor(final CompilationUnit cu) {
      this.resolver = TypeLocator.createDefault(cu);
      this.callbackClass = new NameExpr(Deserialization.class.getName());
    }

    /**
     * This was one of the first ones built, so it does this weird thing that operates on a block
     * and looks inward, rather than detecting a {@link MethodCallExpr} and looking outward. Seems
     * cleaner to do the latter, so we've switched to that pattern primarily.
     */
    @Override
    public Visitable visit(final MethodCallExpr n, final FileWeavingContext context) {
      if (isObjectInputStreamRead(n) && context.isLineIncluded(n)) {
        checkAndWeaveProtectionIfPossible(context, n);
      }
      return super.visit(n, context);
    }

    /**
     * Given an {@link ObjectInputStream#readObject()} call, determine the right type of protection
     * and insert it.
     */
    private void checkAndWeaveProtectionIfPossible(
        final FileWeavingContext context, final MethodCallExpr readObjectInvocation) {
      final MethodDeclaration method = ASTs.findMethodBodyFrom(readObjectInvocation);
      if (method == null) {
        // if there's no method above this, it's not really a valid Java file -- just quit?
        LOG.warn("Found method-less code in {}", ASTs.describeTypeAndMethod(readObjectInvocation));
        return;
      }

      final Expression originalScope = readObjectInvocation.getScope().get();

      // determine if they called setObjectFilter() on this already
      var setObjectInputFilterCallsOnScope =
          method.findAll(MethodCallExpr.class).stream()
              .filter(
                  expr ->
                      expr.getScope().isPresent()
                          && Objects.equals(expr.getScope().get(), originalScope))
              .filter(expr -> "setObjectInputFilter".equals(expr.getNameAsString()))
              .takeWhile(expr -> expr != readObjectInvocation)
              .collect(Collectors.toList());

      var hardeningStatementsOnScope =
          method.findAll(MethodCallExpr.class).stream()
              .filter(expr -> hasFirstArgumentEqualingScope(expr, originalScope))
              .filter(expr -> "enableObjectFilterIfUnprotected".equals(expr.getNameAsString()))
              .takeWhile(expr -> expr != readObjectInvocation)
              .collect(Collectors.toList());

      if (!hardeningStatementsOnScope.isEmpty()) {
        LOG.debug("Already hardened");
        return;
      }

      if (setObjectInputFilterCallsOnScope.size() > 1) {
        LOG.warn(
            "Method had multiple calls to setObjectInputFilter on {} - noping out of here",
            ASTs.describeTypeAndMethod(readObjectInvocation));
      } else if (setObjectInputFilterCallsOnScope.size() == 1) {
        // let's harden if it's not our call that already exists there
        var setObjectInputFilterCall = setObjectInputFilterCallsOnScope.get(0);
        var objectInputFilter = setObjectInputFilterCall.getArgument(0);
        if (objectInputFilter instanceof NameExpr) {
          MethodCallExpr hardenExpression;
          if ("null".equals(objectInputFilter.toString())) {
            hardenExpression = new MethodCallExpr(callbackClass, "hardenedObjectFilter");
          } else {
            hardenExpression =
                new MethodCallExpr(callbackClass, "createCombinedHardenedObjectFilter");
            hardenExpression.addArgument(objectInputFilter);
          }
          setObjectInputFilterCall.setArgument(0, hardenExpression);
        }
      } else { // there's no setObjectInputFilter() call
        // figure out if declared locally
        boolean wasDeclaredOutsideScope =
            originalScope.isNameExpr()
                && method.findAll(VariableDeclarator.class).stream()
                    .filter(this::isNormalObjectInputStreamConstructor)
                    .noneMatch(
                        var -> Objects.equals(originalScope.toString(), var.getNameAsString()));

        // this statement gets the ObjectInputFilter to pass in as an argument to our callback
        var parentNode = readObjectInvocation.getParentNode().get();
        if (parentNode instanceof Statement) {
          injectHardenFilterBeforeBareStatement(
              context, readObjectInvocation, originalScope, (Statement) parentNode);
        } else if (parentNode instanceof VariableDeclarator) {
          if (wasDeclaredOutsideScope) {
            /*
             * This may be a safe subclass of ObjectInputStream -- we can't confirm that, so the best we can
             * do is inject the hardening filter.
             */
            injectHardenFilterBeforeVariableAssigned(
                context, readObjectInvocation, originalScope, (VariableDeclarator) parentNode);
          } else {
            /*
             * Since we know this is a normal ObjectInputStream, and not a safe subclass, we have the choice
             * of either:
             *
             * 1. Replace the ObjectInputStream with a call like: io.pixee.security.Deserialization.createSafeObjectInputStream(is)
             * 2. Inject an ObjectInputFilter.
             */
            injectHardenFilterBeforeVariableAssigned(
                context, readObjectInvocation, originalScope, (VariableDeclarator) parentNode);
          }
        } else {
          // will we ever see this?
          LOG.debug("An unexpected type housing readObject() call: {}", parentNode);
        }
      }
    }

    private boolean hasFirstArgumentEqualingScope(
        final MethodCallExpr expr, final Expression originalScope) {
      var arguments = expr.getArguments();
      if (arguments.size() == 0) {
        return false;
      }
      return arguments.get(0).equals(originalScope);
    }

    private boolean isNormalObjectInputStreamConstructor(
        final VariableDeclarator variableDeclarator) {
      var type = variableDeclarator.getType();
      if (!type.isClassOrInterfaceType()) {
        return false;
      }
      return type.asClassOrInterfaceType().getNameAsString().equals("ObjectInputStream");
    }

    private boolean isObjectInputStreamRead(MethodCallExpr methodCall) {
      return "readObject".equals(methodCall.getNameAsString())
          && methodCall.getScope().isPresent()
          && "java.io.ObjectInputStream".equals(resolver.locateType(methodCall.getScope().get()));
    }

    /**
     * This is for naked calls that don't even assign the deserialized object like:
     *
     * <p>ois.readObject();
     *
     * <p>This is obviously not super realistic, but we should have it in case people are testing
     * our stuff in a PoC kind of situation.
     */
    private void injectHardenFilterBeforeBareStatement(
        final FileWeavingContext context,
        final MethodCallExpr readObjectInvocation,
        final Expression originalScope,
        final Statement vulnerableStatement) {
      var hardeningStmt = generateFilterHardeningStatement(originalScope);
      ASTs.addStatementBeforeStatement(vulnerableStatement, hardeningStmt);
      recordWeave(context, readObjectInvocation);
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
      var expressionStmt = (ExpressionStmt) variableDeclarationExpression.getParentNode().get();
      var hardeningStmt = generateFilterHardeningStatement(originalScope);
      ASTs.addStatementBeforeStatement(expressionStmt, hardeningStmt);
      recordWeave(context, readObjectInvocation);
    }

    private void recordWeave(
        final FileWeavingContext context, final MethodCallExpr readObjectInvocation) {
      context.addWeave(
          Weave.from(readObjectInvocation.getRange().get().begin.line, deserializationRuleId));
    }

    /**
     * Generates an expression to invoke {@link
     * io.pixee.security.Deserialization#enableObjectFilterIfUnprotected(ObjectInputStream)} on the original
     * scope (the {@link ObjectInputStream}).
     */
    private Statement generateFilterHardeningStatement(final Expression originalScope) {
      // this statement is the callback to our hardening code
      var hardenStatement = new MethodCallExpr(callbackClass, "enableObjectFilterIfUnprotected");
      hardenStatement.addArgument(originalScope);
      return new ExpressionStmt(hardenStatement);
    }
  }

  private static final String deserializationRuleId = "pixee:java/deserialization-java-readObject";
  private static final Logger LOG = LogManager.getLogger(DeserializationVisitorFactory.class);
}
