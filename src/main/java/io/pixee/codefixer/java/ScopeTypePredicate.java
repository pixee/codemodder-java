package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

final class ScopeTypePredicate implements Predicate<MethodCallExpr> {

    private final String name;
    private final TypeLocator typeLocator;

    ScopeTypePredicate(final CompilationUnit cu, final String name) {
        Objects.requireNonNull(cu);
        this.typeLocator = TypeLocator.createDefault(cu);
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
        Optional<Expression> scopeRef = methodCallExpr.getScope();
        if(scopeRef.isPresent()) {
            Expression scope = scopeRef.get();
            String scopeTypeName = typeLocator.locateType(scope);
            return name.equals(scopeTypeName);
        }
        return false;
    }
}
