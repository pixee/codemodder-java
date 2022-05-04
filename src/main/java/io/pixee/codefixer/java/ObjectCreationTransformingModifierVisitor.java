package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.protections.TransformationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the glue utility for giving users the ability to gives us a set of {@link Predicate} and
 * a {@link Transformer} for transforming {@link ObjectCreationExpr}.
 */
public class ObjectCreationTransformingModifierVisitor extends ModifierVisitor<FileWeavingContext> {

  private final List<Predicate<ObjectCreationExpr>> predicates;
  private final Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer;

  public ObjectCreationTransformingModifierVisitor(
      final CompilationUnit cu,
      final List<Predicate<ObjectCreationExpr>> predicates,
      final Transformer<ObjectCreationExpr, ObjectCreationExpr> transformer) {
    this.predicates = Objects.requireNonNull(predicates);
    this.transformer = Objects.requireNonNull(transformer);
  }

  @Override
  public Visitable visit(
      final ObjectCreationExpr objectCreationExpr, final FileWeavingContext context) {
    if (objectCreationExpr.hasRange()) {
      for (final Predicate<ObjectCreationExpr> predicate : predicates) {
        if (!predicate.test(objectCreationExpr)) {
          return super.visit(objectCreationExpr, context);
        }
      }
      if (context.isLineIncluded(objectCreationExpr)) {
        try {
          TransformationResult<ObjectCreationExpr> result =
              transformer.transform(objectCreationExpr, context);
          context.addWeave(result.getWeave());
          Optional<ObjectCreationExpr> replacementNode = result.getReplacementNode();
          if (replacementNode.isPresent()) {
            return replacementNode.get();
          }
        } catch (TransformationException e) {
          LOG.error("Problem transforming", e);
        }
      }
    }
    return super.visit(objectCreationExpr, context);
  }

  private static final Logger LOG =
      LogManager.getLogger(ObjectCreationTransformingModifierVisitor.class);
}
