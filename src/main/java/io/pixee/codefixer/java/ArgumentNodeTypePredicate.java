package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Objects;
import java.util.function.Predicate;

final class ArgumentNodeTypePredicate implements Predicate<MethodCallExpr> {

    private final Class<? extends Node> type;
    private int argumentIndex;

    ArgumentNodeTypePredicate(final int argumentIndex, final Class<? extends Node> type) {
        if(argumentIndex < 0) {
            throw new IllegalArgumentException("must be non-negative");
        }
        this.argumentIndex = argumentIndex;
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        if(argumentIndex < arguments.size()) {
            Expression expression = arguments.get(argumentIndex);
            return this.type.isAssignableFrom(expression.getClass());
        }
        return false;
    }
}
