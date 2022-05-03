package io.pixee.codefixer.java;

import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Objects;
import java.util.function.Predicate;

final class MethodNamePredicate implements Predicate<MethodCallExpr> {

    private final String name;

    public MethodNamePredicate(final String name) {
        Objects.requireNonNull(name);
        this.name = name;
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        return name.equals(methodCallExpr.getNameAsString());
    }
}
