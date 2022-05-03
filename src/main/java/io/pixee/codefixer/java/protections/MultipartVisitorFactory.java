package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Objects;

/**
 * This type weaves a protection against path traversal attacks on Apache Multipart library by
 * normalizing the filename pulled from a multipart request.
 */
@Deprecated
public final class MultipartVisitorFactory implements VisitorFactory {

  @Deprecated
  public MultipartVisitorFactory() {
    throw new UnsupportedOperationException("moved to ng");
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new ApacheMultipartVisitory(cu);
  }

    @Override
    public String ruleId() {
        return multipartFilenameSanitizerRuleId;
    }

    private static class ApacheMultipartVisitory extends ModifierVisitor<FileWeavingContext> {

    private final CompilationUnit cu;

    ApacheMultipartVisitory(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final MethodCallExpr n, final FileWeavingContext context) {
      if ("getName".equals(n.getNameAsString()) && n.getScope().isPresent()) {
        Expression scope = n.getScope().get();
        TypeLocator resolver = TypeLocator.createDefault(cu);
        String typeName = resolver.locateType(scope);
        if ("org.apache.commons.fileupload.FileItem".equals(typeName)
            || "org.apache.commons.fileupload.disk.DiskFileItem".equals(typeName)) {
          if (!containsOurCode(n.getParentNode().get())) {
            if (n.getArguments().isEmpty() && context.isLineIncluded(n)) {
              MethodCallExpr safeCall =
                  new MethodCallExpr(
                      new NameExpr(io.pixee.security.SafeIO.class.getName()),
                      "toSimpleFileName",
                      NodeList.nodeList(n));
              context.addWeave(
                  Weave.from(n.getRange().get().begin.line, multipartFilenameSanitizerRuleId));
              return super.visit(safeCall, context);
            }
          }
        }
      } else if ("getOriginalFilename".equals(n.getNameAsString()) && n.getScope().isPresent()) {
        Expression scope = n.getScope().get();
        TypeLocator resolver = TypeLocator.createDefault(cu);
        String typeName = resolver.locateType(scope);
        if ("org.springframework.web.multipart.MultipartFile".equals(typeName)) {
          if (!containsOurCode(n.getParentNode().get())) {
            if (n.getArguments().isEmpty() && context.isLineIncluded(n)) {
              MethodCallExpr safeCall =
                  new MethodCallExpr(
                      new NameExpr(io.pixee.security.SafeIO.class.getName()),
                      "toSimpleFileName",
                      NodeList.nodeList(n));
              context.addWeave(
                  Weave.from(n.getRange().get().begin.line, multipartFilenameSanitizerRuleId));
              return super.visit(safeCall, context);
            }
          }
        }
      }
      return super.visit(n, context);
    }

    private boolean containsOurCode(final Node node) {
      return node.toString().contains("toSimpleFileName");
    }
  }

  private static final String multipartFilenameSanitizerRuleId = "pixee:java/multipart-filename-sanitizer";
}
