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
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/** Registers a converter for common exploit types. */
public final class XStreamVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, CompilationUnit cu) {
    return new XStreamVisitor(cu);
  }

    @Override
    public String ruleId() {
        return xstreamConverterRuleId;
    }

    private static class XStreamVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private XStreamVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final FileWeavingContext context) {
      if (isXStreamCreation(n) && context.isLineIncluded(n) && n.getParentNode().isPresent()) {
        final Node parent = n.getParentNode().get();
        if (parent instanceof VariableDeclarator) {
          final VariableDeclarator variableDeclaration = (VariableDeclarator) parent;
          final String name = variableDeclaration.getNameAsString();
          if (neverCallRegisterConverter(variableDeclaration, name)) {
            Optional<Statement> stmt = ASTs.findParentStatementFrom(variableDeclaration);
            if (stmt.isPresent()) {
              Optional<BlockStmt> blockStmt = ASTs.findBlockStatementFrom(variableDeclaration);
              if (blockStmt.isPresent()) {
                BlockStmt block = blockStmt.get();
                NodeList<Statement> statements = block.getStatements();
                int indexOfVulnStmt = statements.indexOf(stmt.get());
                CompilationUnit parsedFixCode =
                    StaticJavaParser.parse(patchCode.replace("%SCOPE%", name));
                MethodDeclaration method =
                    (MethodDeclaration) parsedFixCode.getChildNodes().get(0).getChildNodes().get(2);
                NodeList<Statement> fixStatements = method.getBody().get().getStatements();
                Statement fixStatement = fixStatements.get(0);
                statements.add(indexOfVulnStmt + 1, fixStatement);
                context.addWeave(Weave.from(n.getRange().get().begin.line, xstreamConverterRuleId));
              }
            }
          }
        }
      }
      return super.visit(n, context);
    }

    private boolean neverCallRegisterConverter(
        final VariableDeclarator variableDeclaration, final String name) {
      boolean calledRegisterConverter =
          ASTs.findMethodBodyFrom(variableDeclaration).findAll(MethodCallExpr.class).stream()
              .filter(
                  methodCallExpr -> "registerConverter".equals(methodCallExpr.getNameAsString()))
              .anyMatch(
                  methodCallExpr ->
                      methodCallExpr.getScope().isPresent()
                          && name.equals(methodCallExpr.getScope().get().toString()));
      return !calledRegisterConverter;
    }

    private boolean isXStreamCreation(final ObjectCreationExpr n) {
      final String typeName = n.getType().getNameAsString();
      return "XStream".equals(typeName) || "com.thoughtworks.xstream.XStream".equals(typeName);
    }
  }

  private static final String patchCode =
      "class Foo { "
          + "XStream xstream; "
          + "void foo() {"
          + "  %SCOPE%.registerConverter(new Converter() {\n"
          + "  public boolean canConvert(final Class type) {\n"
          + "    return type != null && (type == java.beans.EventHandler.class || type == java.lang.ProcessBuilder.class || java.lang.reflect.Proxy.isProxyClass(type));\n"
          + "  }\n"
          + "\n"
          + "  public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {\n"
          + "    throw new SecurityException(\"unsupported type due to security reasons\");\n"
          + "  }\n"
          + "\n"
          + "  public void marshal(final Object source, final HierarchicalStreamWriter writer, final MarshallingContext context) {\n"
          + "    throw new SecurityException(\"unsupported type due to security reasons\");\n"
          + "  }\n"
          + "}, XStream.PRIORITY_LOW);"
          + "}"
          + "}";

  private static final String xstreamConverterRuleId = "pixee:java/xstream-harden-converter";
}
