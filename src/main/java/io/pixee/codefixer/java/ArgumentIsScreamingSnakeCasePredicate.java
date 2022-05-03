package io.pixee.codefixer.java;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.function.Predicate;
import java.util.regex.Pattern;

final class ArgumentIsScreamingSnakeCasePredicate implements Predicate<MethodCallExpr> {

    private final int argumentIndex;

    ArgumentIsScreamingSnakeCasePredicate(final int argumentIndex) {
        if(argumentIndex < 0) {
            throw new IllegalArgumentException("must be non-negative");
        }
        this.argumentIndex = argumentIndex;
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();
        if(argumentIndex < arguments.size()) {
            Expression argument = arguments.get(argumentIndex);
            return argument.isNameExpr() && looksLikeConstant(argument.toString());
        }
        return false;
    }


    private boolean looksLikeConstant(final String variableName) {
        return commonConstantPattern.matcher(variableName).matches();
    }

    private static final Pattern commonConstantPattern = Pattern.compile("[A-Z_]{2,}");
}
