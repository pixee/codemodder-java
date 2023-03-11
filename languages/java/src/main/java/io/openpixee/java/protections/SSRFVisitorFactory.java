package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.codemodder.DependencyGAV;
import io.codemodder.Weave;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.security.*;
import java.io.File;
import java.util.Objects;

/**
 * Offers protective wrapper API when creating {@link java.net.URL} instances to prevent SSRF
 * attacks.
 */
public final class SSRFVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new SSRFVisitor(cu);
  }

  @Override
  public String ruleId() {
    return ssrfRuleId;
  }

  private static class SSRFVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private SSRFVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final FileWeavingContext context) {
      if (n.getType().isClassOrInterfaceType()) {
        ClassOrInterfaceType type = n.getType().asClassOrInterfaceType();
        if (type.getNameAsString().equals("URL") || type.getNameAsString().equals("java.net.URL")) {
          if (!n.getArguments().isEmpty()) {
            if (!hasAllConstantArguments(n.getArguments()) && context.isLineIncluded(n)) {
              /*
               * We need to replace:
               *
               * URL u = new URL(foo)
               *
               * With:
               *
               * URL u = io.openpixee.security.Urls.create(foo, io.openpixee.security.Urls.HTTP_PROTOCOLS, io.openpixee.security.HostValidator.ALLOW_ALL)
               */
              ASTTransforms.addImportIfMissing(cu, Urls.class);
              ASTTransforms.addImportIfMissing(cu, HostValidator.class);
              FieldAccessExpr httpProtocolsExpr = new FieldAccessExpr();
              httpProtocolsExpr.setScope(new NameExpr(Urls.class.getSimpleName()));
              httpProtocolsExpr.setName("HTTP_PROTOCOLS");

              FieldAccessExpr denyCommonTargetsExpr = new FieldAccessExpr();

              denyCommonTargetsExpr.setScope(new NameExpr(HostValidator.class.getSimpleName()));
              denyCommonTargetsExpr.setName("DENY_COMMON_INFRASTRUCTURE_TARGETS");

              NodeList<Expression> newArguments = new NodeList<>();
              newArguments.addAll(
                  n.getArguments()); // first are all the arguments they were passing to "new URL"
              newArguments.add(httpProtocolsExpr); // load the protocols they're allowed
              newArguments.add(denyCommonTargetsExpr); // load the host validator
              MethodCallExpr safeCall =
                  new MethodCallExpr(
                      new NameExpr(io.openpixee.security.Urls.class.getSimpleName()),
                      "create",
                      newArguments);
              context.addWeave(
                  Weave.from(
                      n.getRange().get().begin.line,
                      ssrfRuleId,
                      DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT));
              return super.visit(safeCall, context);
            }
          }
        }
      }
      return super.visit(n, context);
    }

    /** If the call to create a URL is all constant values, there's no risk of injection... */
    private boolean hasAllConstantArguments(final NodeList<Expression> arguments) {
      if (arguments.get(0).isStringLiteralExpr()) {
        if (arguments.size() > 1) {
          return arguments.get(1).isStringLiteralExpr();
        } else {
          return true;
        }
      }
      return false;
    }
  }

  private static final String ssrfRuleId = "pixee:java/sandbox-url-creation";
}
