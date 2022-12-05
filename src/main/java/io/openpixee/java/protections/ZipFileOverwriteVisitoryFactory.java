package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.openpixee.java.DependencyGAV;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.ObjectCreationPredicateFactory;
import io.openpixee.java.ObjectCreationToMethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.Weave;
import io.openpixee.security.ZipSecurity;
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
            NameExpr callbackClass = new NameExpr(ZipSecurity.class.getSimpleName());
            final MethodCallExpr securedCall =
                new MethodCallExpr(callbackClass, "createHardenedInputStream");
            securedCall.setArguments(objectCreationExpr.getArguments());
            // TODO treat this optional here
            final CompilationUnit cu = objectCreationExpr.findCompilationUnit().get();
            ASTs.addImportIfMissing(cu, ZipSecurity.class);
            Weave weave =
                Weave.from(
                    objectCreationExpr.getRange().get().begin.line,
                    zipHardeningRuleId,
                    DependencyGAV.OPENPIXEE_JAVA_SECURITY_TOOLKIT);
            return new TransformationResult<>(Optional.of(securedCall), weave);
          }
        };

    return new ObjectCreationToMethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return zipHardeningRuleId;
  }

  private static final String zipHardeningRuleId = "pixee:java/harden-zip-entry-paths";
}
