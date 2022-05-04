package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationPredicateFactory;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Hardens {@link java.beans.XMLDecoder} constructor calls to use a reader that will notice when common exploit targets
 * are being read.
 */
public final class XMLDecoderVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<ObjectCreationExpr>> predicates = List.of(
            ObjectCreationPredicateFactory.withArgumentCount(0).negate(),
            ObjectCreationPredicateFactory.withArgumentType(cu, 0, "java.io.InputStream"),
            ObjectCreationPredicateFactory.withType("XMLDecoder").or(ObjectCreationPredicateFactory.withType("java.beans.XMLDecoder"))
    );

    Transformer<ObjectCreationExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<ObjectCreationExpr> transform(final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
        MethodCallExpr safeExpr =
                new MethodCallExpr(
                        new NameExpr(io.pixee.security.SafeIO.class.getName()), "toSafeXmlDecoderInputStream");
        final Expression firstArgument = objectCreationExpr.getArgument(0);
        safeExpr.setArguments(NodeList.nodeList(firstArgument));
        objectCreationExpr.setArgument(0, safeExpr);
        Weave weave = Weave.from(objectCreationExpr.getRange().get().begin.line, hardenXmlDecoderRuleId);
        return new TransformationResult<>(Optional.empty(), weave);
      }
    };

    return new ObjectCreationTransformingModifierVisitor(cu, predicates, transformer);
  }

    @Override
    public String ruleId() {
        return hardenXmlDecoderRuleId;
    }

  private static final String hardenXmlDecoderRuleId = "pixee:java/harden-xmldecoder";
}
