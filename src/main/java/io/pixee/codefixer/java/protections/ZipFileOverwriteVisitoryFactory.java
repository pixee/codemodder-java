package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationToMethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import io.pixee.security.Zip;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This type prevents zip file overwrite attacks (e.g., "zip slip") by rewriting the creation of
 * {@link java.util.zip.ZipInputStream} into a hardened version.
 */
public final class ZipFileOverwriteVisitoryFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<ObjectCreationExpr>> predicates = List.of(
            ObjectCreationPredicateFactory.withType("ZipInputStream").or(ObjectCreationPredicateFactory.withType("java.util.zip.ZipInputStream"))
    );

    Transformer<ObjectCreationExpr, MethodCallExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<MethodCallExpr> transform(final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
        NameExpr callbackClass = new NameExpr(Zip.class.getName());
        final MethodCallExpr securedCall = new MethodCallExpr(callbackClass, "createHardenedZipInputStream");
        securedCall.setArguments(objectCreationExpr.getArguments());
        Weave weave = Weave.from(objectCreationExpr.getRange().get().begin.line, zipHardeningRuleId);
        return new TransformationResult<>(Optional.of(securedCall), weave);
      }
    };

    return new ObjectCreationToMethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

    @Override
    public String ruleId() {
        return zipHardeningRuleId;
    }

    private static class ZipFileOverwriteVisitor extends ModifierVisitor<FileWeavingContext> {
    private final NameExpr callbackClass;

    private ZipFileOverwriteVisitor() {
      this.callbackClass = new NameExpr(Zip.class.getName());
    }

    @Override
    public Visitable visit(final ObjectCreationExpr n, final FileWeavingContext context) {
      if (context.isLineIncluded(n)) {
        if (n.getParentNode().isPresent()) {
          if (n.getType().isClassOrInterfaceType()) {
            final ClassOrInterfaceType type = n.getType().asClassOrInterfaceType();
            if (type.getNameAsString().equals("ZipInputStream")
                || type.getNameAsString().equals("java.util.zip.ZipInputStream")) {
              final MethodCallExpr securedCall =
                  new MethodCallExpr(callbackClass, "createHardenedZipInputStream");
              securedCall.setArguments(n.getArguments());
              context.addWeave(Weave.from(n.getRange().get().begin.line, zipHardeningRuleId));
              return super.visit(securedCall, context);
            }
          }
        }
      }
      return super.visit(n, context);
    }
  }

  private static final String zipHardeningRuleId = "pixee:java/zip-hardening";
}
