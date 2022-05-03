package io.pixee.codefixer.java;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Objects;
import java.util.function.Predicate;

final class ArgumentCodeContainsPredicate implements Predicate<MethodCallExpr> {

    private final int argumentIndex;
    private final String searchStr;

    ArgumentCodeContainsPredicate(final int argumentIndex, final String searchStr) {
        if(argumentIndex < 0) {
            throw new IllegalArgumentException("must be non-negative");
        }
        this.argumentIndex = argumentIndex;
        this.searchStr = Objects.requireNonNull(searchStr);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        if(argumentIndex < arguments.size()) {
            Expression argument = methodCallExpr.getArgument(argumentIndex);
            String argumentAsCodeString = argument.toString();
            return argumentAsCodeString != null && argumentAsCodeString.contains(searchStr);
        }
        return false;
    }
}
