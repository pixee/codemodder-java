package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Objects;
import java.util.function.Predicate;

final class ArgumentTypePredicate implements Predicate<MethodCallExpr> {

    private final String type;
    private int argumentIndex;
    private final TypeLocator typeLocator;

    ArgumentTypePredicate(final CompilationUnit cu, final int argumentIndex, final String type) {
        if(argumentIndex < 0) {
            throw new IllegalArgumentException("must be non-negative");
        }
        this.argumentIndex = argumentIndex;
        this.type = Objects.requireNonNull(type);
        this.typeLocator = TypeLocator.createDefault(cu);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        if(argumentIndex < arguments.size()) {
            Expression expression = arguments.get(argumentIndex);
            String type = typeLocator.locateType(expression);
            return this.type.equals(type);
        }
        return false;
    }
}
