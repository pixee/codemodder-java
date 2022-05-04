package io.pixee.codefixer.java.protections;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import javassist.compiler.ast.MethodDecl;

import java.io.File;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/** Registers a converter for common exploit types. */
public final class XStreamVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    List<Predicate<ObjectCreationExpr>> predicates = List.of(
            ObjectCreationPredicateFactory.withArgumentCount(0),
            ObjectCreationPredicateFactory.withType("XStream").or(ObjectCreationPredicateFactory.withType("com.thoughtworks.xstream.XStream")),
            ObjectCreationPredicateFactory.withParentType(VariableDeclarator.class),
            new Predicate<>() {
              @Override
              public boolean test(final ObjectCreationExpr objectCreationExpr) {
                final VariableDeclarator variableDeclaration = (VariableDeclarator) objectCreationExpr.getParentNode().get();
                final String name = variableDeclaration.getNameAsString();
                return neverCallRegisterConverter(variableDeclaration, name);
              }

              private boolean neverCallRegisterConverter(
                      final VariableDeclarator variableDeclaration, final String name) {
                Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(variableDeclaration);
                if (methodRef.isPresent()) {
                  MethodDeclaration method = methodRef.get();
                  boolean calledRegisterConverter =
                          method.findAll(MethodCallExpr.class).stream()
                                  .filter(
                                          methodCallExpr -> "registerConverter".equals(methodCallExpr.getNameAsString()))
                                  .anyMatch(
                                          methodCallExpr ->
                                                  methodCallExpr.getScope().isPresent()
                                                          && name.equals(methodCallExpr.getScope().get().toString()));
                  return !calledRegisterConverter;
                }
                return false;
              }
            }
    );

    Transformer<ObjectCreationExpr,ObjectCreationExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<ObjectCreationExpr> transform(final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
        VariableDeclarator variableDeclaration = (VariableDeclarator) objectCreationExpr.getParentNode().get();
        Optional<Statement> stmt = ASTs.findParentStatementFrom(variableDeclaration);
        if (stmt.isPresent()) {
          Optional<BlockStmt> blockStmt = ASTs.findBlockStatementFrom(variableDeclaration);
          if (blockStmt.isPresent()) {
            BlockStmt block = blockStmt.get();
            NodeList<Statement> statements = block.getStatements();
            int indexOfVulnStmt = statements.indexOf(stmt.get());
            statements.add(indexOfVulnStmt + 1, buildFixStatement(variableDeclaration.getNameAsString()));
            context.addWeave(Weave.from(objectCreationExpr.getRange().get().begin.line, xstreamConverterRuleId));
          }
        }
        Weave weave = Weave.from(objectCreationExpr.getRange().get().begin.line, xstreamConverterRuleId);
        return new TransformationResult<>(Optional.empty(), weave);
      }
    };

    return new ObjectCreationTransformingModifierVisitor(cu, predicates, transformer);
  }

    @Override
    public String ruleId() {
        return xstreamConverterRuleId;
    }

  private static Statement buildFixStatement(final String name) {
    CompilationUnit parsedFixCode =
            StaticJavaParser.parse(patchCode.replace("%SCOPE%", name));
    MethodDeclaration method =
            (MethodDeclaration) parsedFixCode.getChildNodes().get(0).getChildNodes().get(2);
    NodeList<Statement> fixStatements = method.getBody().get().getStatements();
    return fixStatements.get(0);
  }

  private static final String patchCode =
      "class Foo { "
          + "XStream xstream; "
          + "void foo() {"
          + "  %SCOPE%.registerConverter(new com.thoughtworks.xstream.converters.Converter() {\n"
          + "  public boolean canConvert(final Class type) {\n"
          + "    return type != null && (type == java.beans.EventHandler.class || type == java.lang.ProcessBuilder.class || java.lang.reflect.Proxy.isProxyClass(type));\n"
          + "  }\n"
          + "\n"
          + "  public Object unmarshal(final com.thoughtworks.xstream.io.HierarchicalStreamReader reader, final com.thoughtworks.xstream.converters.UnmarshallingContext context) {\n"
          + "    throw new SecurityException(\"unsupported type due to security reasons\");\n"
          + "  }\n"
          + "\n"
          + "  public void marshal(final Object source, final com.thoughtworks.xstream.io.HierarchicalStreamWriter writer, final com.thoughtworks.xstream.converters.MarshallingContext context) {\n"
          + "    throw new SecurityException(\"unsupported type due to security reasons\");\n"
          + "  }\n"
          + "}, XStream.PRIORITY_LOW);"
          + "}"
          + "}";

  private static final String xstreamConverterRuleId = "pixee:java/xstream-harden-converter";
}
