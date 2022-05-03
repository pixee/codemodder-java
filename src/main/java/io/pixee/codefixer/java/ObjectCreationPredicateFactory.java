package io.pixee.codefixer.java;

import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * This is meant to be consumed by rule writers to help them reduce boilerplate and reduce mistakes when writing code
 * to match certain conditions of code to match.
 */
public interface ObjectCreationPredicateFactory {

    static Predicate<ObjectCreationExpr> withType(final String type) {
        return new FullyQualifiedNamePredicate(type);
    }

    static Predicate<ObjectCreationExpr> withArgumentCount(final int argumentCount) {
        return new ArgumentCountPredicate(argumentCount);
    }

    final class FullyQualifiedNamePredicate implements Predicate<ObjectCreationExpr> {

        private final String type;

        private FullyQualifiedNamePredicate(final String type) {
            this.type = Objects.requireNonNull(type);
        }

        @Override
        public boolean test(final ObjectCreationExpr objectCreationExpr) {
            ClassOrInterfaceType type = objectCreationExpr.getType();
            return this.type.equals(type.getNameAsString());
        }
    }

    final class ArgumentCountPredicate implements Predicate<ObjectCreationExpr> {

        private final int argumentCount;

        private ArgumentCountPredicate(final int argumentCount) {
            if(argumentCount < 0) {
                throw new IllegalArgumentException("must be non-negative");
            }
            this.argumentCount = argumentCount;
        }

        @Override
        public boolean test(final ObjectCreationExpr objectCreationExpr) {
            return objectCreationExpr.getArguments().size() == argumentCount;
        }
    }
}
