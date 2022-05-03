package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.codefixer.java.protections.TransformationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This is the glue utility for giving users the ability to gives us a set of {@link Predicate} and a {@link Transformer}
 * for transforming {@link MethodCallExpr}.
 */
public class MethodCallTransformingModifierVisitor extends ModifierVisitor<FileWeavingContext> {

    private final Set<Predicate<MethodCallExpr>> predicates;
    private final Transformer<MethodCallExpr> transformer;

    public MethodCallTransformingModifierVisitor(final CompilationUnit cu, final Set<Predicate<MethodCallExpr>> predicates, final Transformer<MethodCallExpr> transformer) {
        this.predicates = Objects.requireNonNull(predicates);
        this.transformer = Objects.requireNonNull(transformer);
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
        if(methodCallExpr.hasScope() && methodCallExpr.hasRange()) {
            for (final Predicate<MethodCallExpr> predicate : predicates) {
                if (!predicate.test(methodCallExpr)) {
                    return super.visit(methodCallExpr, context);
                }
            }
            if(context.isLineIncluded(methodCallExpr)) {
                try {
                    TransformationResult<MethodCallExpr> result = transformer.transform(methodCallExpr, context);
                    context.addWeave(result.getWeave());
                    Optional<MethodCallExpr> replacementNode = result.getReplacementNode();
                    if(replacementNode.isPresent()) {
                        return replacementNode.get();
                    }
                } catch (TransformationException e) {
                    LOG.error("Problem transforming", e);
                }
            }
        }
        return super.visit(methodCallExpr, context);
    }

    private static final Logger LOG = LogManager.getLogger(MethodCallTransformingModifierVisitor.class);
}
