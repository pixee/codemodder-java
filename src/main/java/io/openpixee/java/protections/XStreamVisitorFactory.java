package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.ObjectCreationPredicateFactory;
import io.openpixee.java.ObjectCreationTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.Weave;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Registers a converter for common exploit types. */
public final class XStreamVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    List<Predicate<ObjectCreationExpr>> predicates =
        List.of(
            ObjectCreationPredicateFactory.withArgumentCount(0),
            ObjectCreationPredicateFactory.withType("XStream")
                .or(ObjectCreationPredicateFactory.withType("com.thoughtworks.xstream.XStream")),
            ObjectCreationPredicateFactory.withParentType(VariableDeclarator.class),
            new Predicate<>() {
              @Override
              public boolean test(final ObjectCreationExpr objectCreationExpr) {
                final VariableDeclarator variableDeclaration =
                    (VariableDeclarator) objectCreationExpr.getParentNode().get();
                final String name = variableDeclaration.getNameAsString();
                return neverLimitedTypes(variableDeclaration, name);
              }

              private boolean neverLimitedTypes(
                  final VariableDeclarator variableDeclaration, final String name) {
                Optional<MethodDeclaration> methodRef =
                    ASTs.findMethodBodyFrom(variableDeclaration);
                if (methodRef.isPresent()) {
                  MethodDeclaration method = methodRef.get();
                  boolean calledRegisterConverter =
                      method.findAll(MethodCallExpr.class).stream()
                          .filter(
                              methodCallExpr ->
                                  "registerConverter".equals(methodCallExpr.getNameAsString()))
                          .anyMatch(
                              methodCallExpr ->
                                  methodCallExpr.getScope().isPresent()
                                      && name.equals(methodCallExpr.getScope().get().toString()));
                  return !calledRegisterConverter;
                }
                return false;
              }
            });

    Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<ObjectCreationExpr> transform(
              final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
            VariableDeclarator variableDeclaration =
                (VariableDeclarator) objectCreationExpr.getParentNode().get();
            Optional<Statement> stmt = ASTs.findParentStatementFrom(variableDeclaration);
            if (stmt.isPresent()) {
              Optional<BlockStmt> blockStmt = ASTs.findBlockStatementFrom(variableDeclaration);
              if (blockStmt.isPresent()) {
                BlockStmt block = blockStmt.get();
                NodeList<Statement> statements = block.getStatements();
                int indexOfVulnStmt = statements.indexOf(stmt.get());
                statements.add(
                    indexOfVulnStmt + 1, buildFixStatement(variableDeclaration.getNameAsString()));
                ASTTransforms.addImportIfMissing(cu, "io.openpixee.security.HardeningConverter");
                context.addWeave(
                    Weave.from(
                        objectCreationExpr.getRange().get().begin.line, xstreamConverterRuleId));
              }
            }
            Weave weave =
                Weave.from(objectCreationExpr.getRange().get().begin.line, xstreamConverterRuleId);
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new ObjectCreationTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return xstreamConverterRuleId;
  }

  private static Statement buildFixStatement(final String variableName) {
    ExpressionStmt newStatement = new ExpressionStmt();
    ObjectCreationExpr hardeningConverter = new ObjectCreationExpr();
    hardeningConverter.setType(new ClassOrInterfaceType("HardeningConverter"));
    MethodCallExpr registerConverterCall =
        new MethodCallExpr("registerConverter", hardeningConverter);
    newStatement.setExpression(registerConverterCall);
    registerConverterCall.setScope(new NameExpr(variableName));
    return newStatement;
  }

  private static final String xstreamConverterRuleId = "pixee:java/harden-xstream";
}
