package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import io.openpixee.security.XMLInputFactorySecurity;
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

    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("newFactory")
                .or(MethodCallPredicateFactory.withName("newInstance")),
            MethodCallPredicateFactory.withScopeType(cu, "javax.xml.stream.XMLInputFactory"),
            MethodCallPredicateFactory.withArgumentCount(0)
                .or(MethodCallPredicateFactory.withArgumentCount(2)),
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
                  final Optional<MethodDeclaration> methodRef =
                      ASTs.findMethodBodyFrom(variableDeclarator);
                  if (methodRef.isEmpty()) {
                    return false;
                  }
                  final MethodDeclaration method = methodRef.get();
                  final BlockStmt methodBody = method.getBody().get();
                  return methodBody.findAll(MethodCallExpr.class).stream()
                      .filter(
                          methodCallExpr ->
                              "hardenFactory".equals(methodCallExpr.getNameAsString()))
                      .filter(methodCallExpr -> methodCallExpr.getArguments().size() == 1)
                      .filter(methodCallExpr -> methodCallExpr.getArgument(0).equals(scope))
                      .findAny()
                      .isEmpty();
                } else if (parentNode instanceof MethodCallExpr) {
                  final MethodCallExpr wrappedCall = (MethodCallExpr) parentNode;
                  return !wrappedCall.getNameAsString().equals("hardenFactory");
                }
                return true;
              }
            });

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            ASTTransforms.addImportIfMissing(cu, XMLInputFactorySecurity.class);
            final NameExpr callbackClass =
                new NameExpr(XMLInputFactorySecurity.class.getSimpleName());
            final MethodCallExpr wrapperExpr = new MethodCallExpr(callbackClass, "hardenFactory");
            wrapperExpr.setArguments(NodeList.nodeList(methodCallExpr));
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    hardenXmlInputFactoryCode,
                    DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
            return new TransformationResult<>(Optional.of(wrapperExpr), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return hardenXmlInputFactoryCode;
  }

  private static final String hardenXmlInputFactoryCode = "pixee:java/harden-xmlinputfactory";
}
