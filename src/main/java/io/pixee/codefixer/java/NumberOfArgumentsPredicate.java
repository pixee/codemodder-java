package io.pixee.codefixer.java;

import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.function.Predicate;

final class NumberOfArgumentsPredicate implements Predicate<MethodCallExpr> {

    private final int numberOfArguments;

    NumberOfArgumentsPredicate(final int numberOfArguments) {
        if (numberOfArguments < 0) {
            throw new IllegalArgumentException("must be non-negative");
        }
        this.numberOfArguments = numberOfArguments;
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        return numberOfArguments == methodCallExpr.getArguments().size();
    }
}
