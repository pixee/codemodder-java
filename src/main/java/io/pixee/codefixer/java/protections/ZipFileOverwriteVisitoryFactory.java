package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationToMethodCallTransformingModifierVisitor;
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
    List<Predicate<ObjectCreationExpr>> predicates =
        List.of(
            ObjectCreationPredicateFactory.withType("ZipInputStream")
                .or(ObjectCreationPredicateFactory.withType("java.util.zip.ZipInputStream")));

    Transformer<ObjectCreationExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
            NameExpr callbackClass = new NameExpr(Zip.class.getName());
            final MethodCallExpr securedCall =
                new MethodCallExpr(callbackClass, "createHardenedZipInputStream");
            securedCall.setArguments(objectCreationExpr.getArguments());
            Weave weave =
                Weave.from(objectCreationExpr.getRange().get().begin.line, zipHardeningRuleId);
            return new TransformationResult<>(Optional.of(securedCall), weave);
          }
        };

    return new ObjectCreationToMethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return zipHardeningRuleId;
  }

  private static final String zipHardeningRuleId = "pixee:java/zip-hardening";
}
