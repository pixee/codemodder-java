package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import java.io.File;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * This visitor replaces instance creation of {@link java.util.Random} with {@link
 * java.security.SecureRandom}.
 */
public final class WeakPRNGVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    return new WeakPRNGVisitor(cu);
  }

  @Override
  public String ruleId() {
      return secureRandomRuleId;
  }

  private static class WeakPRNGVisitor extends ModifierVisitor<FileWeavingContext> {
    private final CompilationUnit cu;

    private WeakPRNGVisitor(final CompilationUnit cu) {
      this.cu = Objects.requireNonNull(cu);
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final FileWeavingContext context) {

      if (n.getType().isClassOrInterfaceType()) {
        ClassOrInterfaceType type = n.getType().asClassOrInterfaceType();
        if (type.getNameAsString().equals("Random")
                || type.getNameAsString().equals("java.util.Random")) {
          if (n.getArguments().isEmpty() && context.isLineIncluded(n)) {
            n.setType(new ClassOrInterfaceType(SecureRandom.class.getName()));
            context.addWeave(Weave.from(n.getRange().get().begin.line, secureRandomRuleId));
          }
        }
      }
      return super.visit(n, context);
    }
  }

  private static final String secureRandomRuleId = "pixee:java/secure-random";
}
