package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;

public final class XMLDecoderVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new XMLDecoderVisitor(cu);
  }

    @Override
    public String ruleId() {
        return hardenXmlDecoderRuleId;
    }

    private static final class XMLDecoderVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private XMLDecoderVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final FileWeavingContext context) {
      if (n.getType().isClassOrInterfaceType() && context.isLineIncluded(n)) {
        ClassOrInterfaceType type = n.getType().asClassOrInterfaceType();
        if (type.getNameAsString().equals("XMLDecoder")
            || type.getNameAsString().equals("java.beans.XMLDecoder")) {
          if (!n.getArguments().isEmpty()) {
            final Expression firstArgument = n.getArgument(0);
            final TypeLocator resolver = TypeLocator.createDefault(cu);
            final String firstArgumentType = resolver.locateType(firstArgument);
            if (firstArgumentType != null && firstArgumentType.endsWith("InputStream")) {
              MethodCallExpr safeExpr =
                  new MethodCallExpr(
                      new NameExpr(io.pixee.security.SafeIO.class.getName()), "toSafeXmlDecoderInputStream");
              safeExpr.setArguments(NodeList.nodeList(firstArgument));
              n.setArgument(0, safeExpr);
              context.addWeave(Weave.from(n.getRange().get().begin.line, hardenXmlDecoderRuleId));
            }
          }
        }
      }
      return super.visit(n, context);
    }
  }

  private static final String hardenXmlDecoderRuleId = "pixee:java/harden-xmldecoder";
}
