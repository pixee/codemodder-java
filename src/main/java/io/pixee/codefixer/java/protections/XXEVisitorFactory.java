package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.TransformationException;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This visitor prevents XXE attacks by injecting the correct flags into XML parsers at creation or
 * at use.
 */
public final class XXEVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {

    List<Predicate<MethodCallExpr>> predicates = List.of(
            MethodCallPredicateFactory.withName("newFactory").or(MethodCallPredicateFactory.withName("newInstance")),
            MethodCallPredicateFactory.withScopeType(cu, "javax.xml.stream.XMLInputFactory"),
            MethodCallPredicateFactory.withArgumentCount(0).or(MethodCallPredicateFactory.withArgumentCount(2)),
            new Predicate<MethodCallExpr>() {
              @Override
              public boolean test(final MethodCallExpr newFactoryCreationCall) {
                if (newFactoryCreationCall.getParentNode().isEmpty()) {
                  return false;
                }
                Node parentNode = newFactoryCreationCall.getParentNode().get();
                if (parentNode instanceof VariableDeclarator) {
                  final VariableDeclarator variableDeclarator = (VariableDeclarator) parentNode;
                  final NameExpr scope = variableDeclarator.getNameAsExpression();
                  final Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(variableDeclarator);
                  if (methodRef.isEmpty()) {
                    return false;
                  }
                  final MethodDeclaration method = methodRef.get();
                  final BlockStmt methodBody = method.getBody().get();
                  return methodBody.findAll(MethodCallExpr.class).stream()
                          .filter(
                                  methodCallExpr -> "hardenXmlInputFactory".equals(methodCallExpr.getNameAsString()))
                          .filter(methodCallExpr -> methodCallExpr.getArguments().size() == 1)
                          .filter(methodCallExpr -> methodCallExpr.getArgument(0).equals(scope))
                          .findAny()
                          .isEmpty();
                } else if (parentNode instanceof MethodCallExpr) {
                  final MethodCallExpr wrappedCall = (MethodCallExpr) parentNode;
                  return !wrappedCall.getNameAsString().equals("hardenXmlInputFactory");
                }
                return true;
              }
            }
    );

    Transformer<MethodCallExpr,MethodCallExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<MethodCallExpr> transform(final MethodCallExpr methodCallExpr, final FileWeavingContext context) throws TransformationException {
        final NameExpr callbackClass = new NameExpr(io.pixee.security.XXE.class.getName());
        final MethodCallExpr wrapperExpr = new MethodCallExpr(callbackClass, "hardenXmlInputFactory");
        wrapperExpr.setArguments(NodeList.nodeList(methodCallExpr));
        Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, hardenXmlInputFactoryCode);
        return new TransformationResult<>(Optional.of(wrapperExpr), weave);
      }
    };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

    @Override
    public String ruleId() {
        return hardenXmlInputFactoryCode;
    }

    private static class XXEVisitor extends ModifierVisitor<FileWeavingContext> {
    private final TypeLocator resolver;

    private XXEVisitor(final CompilationUnit cu) {
      this.resolver = TypeLocator.createDefault(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      // wrap calls to XMLInputFactory.newDefaultFactory() / newInstance() / newFactory() /
      // newInstance(string,classloader)
      if ("newFactory".equals(methodCallExpr.getNameAsString())
          || "newInstance".equals(methodCallExpr.getNameAsString())) {
        if (context.isLineIncluded(methodCallExpr) && methodCallExpr.getScope().isPresent()) {
          if ("javax.xml.stream.XMLInputFactory"
              .equals(resolver.locateType(methodCallExpr.getScope().get()))) {
            if (methodCallExpr.getParentNode().isPresent()
                && isntHardenedLater(methodCallExpr.getParentNode().get())) {
              if (methodCallExpr.getArguments().isEmpty()
                  || methodCallExpr.getArguments().size() == 2) {
                return weave(methodCallExpr, methodCallExpr.getParentNode().get(), context);
              }
            }
          }
        }
      }
      return super.visit(methodCallExpr, context);
    }

      private boolean isntHardenedLater(final Node parentNode) {
        if (parentNode instanceof VariableDeclarator) {
          final VariableDeclarator variableDeclarator = (VariableDeclarator) parentNode;
          final NameExpr scope = variableDeclarator.getNameAsExpression();
          final Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(variableDeclarator);
          if(methodRef.isEmpty()) {
            return false;
          }
          final MethodDeclaration method = methodRef.get();
          final BlockStmt methodBody = method.getBody().get();
          return methodBody.findAll(MethodCallExpr.class).stream()
                  .filter(
                          methodCallExpr -> "hardenXmlInputFactory".equals(methodCallExpr.getNameAsString()))
                  .filter(methodCallExpr -> methodCallExpr.getArguments().size() == 1)
                  .filter(methodCallExpr -> methodCallExpr.getArgument(0).equals(scope))
                  .findAny()
                  .isEmpty();
        } else if (parentNode instanceof MethodCallExpr) {
          final MethodCallExpr wrappedCall = (MethodCallExpr) parentNode;
          return !wrappedCall.getNameAsString().equals("hardenXmlInputFactory");
        }
        return true;
      }

    private MethodCallExpr weave(
        final MethodCallExpr methodCallExpr,
        final Node parentNode,
        final FileWeavingContext context) {
      final NameExpr callbackClass = new NameExpr(io.pixee.security.XXE.class.getName());
      final MethodCallExpr wrapperExpr = new MethodCallExpr(callbackClass, "hardenXmlInputFactory");
      wrapperExpr.setArguments(NodeList.nodeList(methodCallExpr));
      context.addWeave(
          Weave.from(methodCallExpr.getRange().get().begin.line, hardenXmlInputFactoryCode));
      return wrapperExpr;
    }
  }

  private static final String hardenXmlInputFactoryCode = "pixee:java/harden-xmlinputfactory";
}
