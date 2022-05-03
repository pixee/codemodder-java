package io.pixee.codefixer.java;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.pixee.codefixer.java.protections.ASTs;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class MethodPreviouslyCalledOnScope implements Predicate<MethodCallExpr> {

    private String methodName;

    MethodPreviouslyCalledOnScope(final String methodName) {
        this.methodName = Objects.requireNonNull(methodName);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        Optional<Expression> scopeRef = methodCallExpr.getScope();
        if(scopeRef.isEmpty()) {
            return false;
        }
        Expression scope = scopeRef.get();
        Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(methodCallExpr);
        if(methodRef.isEmpty()) {
            return false;
        }
        MethodDeclaration method = methodRef.get();
        var previousMethodCallsOnScope =
                method.findAll(MethodCallExpr.class).stream()
                        .filter(
                                expr ->
                                        expr.getScope().isPresent()
                                                && Objects.equals(expr.getScope().get(), scope))
                        .filter(expr -> methodName.equals(expr.getNameAsString()))
                        .takeWhile(expr -> expr != methodCallExpr)
                        .collect(Collectors.toList());

        return !previousMethodCallsOnScope.isEmpty();
    }
}
