package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.pixee.codefixer.java.protections.ASTs;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This is meant to be consumed by rule writers to help them reduce boilerplate and reduce mistakes
 * when writing code to match certain conditions of code to match.
 */
public interface MethodCallPredicateFactory {

  static Predicate<MethodCallExpr> withArgumentCount(final int numberOfArguments) {
    return new NumberOfArgumentsPredicate(numberOfArguments);
  }

  static Predicate<MethodCallExpr> withArgumentCodeContains(
      final int argumentIndex, final String searchStr) {
    return new ArgumentCodeContainsPredicate(argumentIndex, searchStr);
  }

  static Predicate<MethodCallExpr> withParentCodeContains(final String searchStr) {
    return new ParentCodeContainsPredicate(searchStr);
  }

  static Predicate<MethodCallExpr> withName(final String name) {
    return new MethodNamePredicate(name);
  }

  static Predicate<MethodCallExpr> withScopeType(final CompilationUnit cu, final String name) {
    return new ScopeTypePredicate(cu, name);
  }

  static Predicate<MethodCallExpr> withArgumentType(
      final CompilationUnit cu, final int argumentIndex, final String name) {
    return new ArgumentTypePredicate(cu, argumentIndex, name);
  }

  static Predicate<MethodCallExpr> withArgumentNodeType(
      final int argumentIndex, final Class<? extends Node> nodeType) {
    return new ArgumentNodeTypePredicate(argumentIndex, nodeType);
  }

  static Predicate<MethodCallExpr> withScreamingSnakeCaseVariableNameForArgument(
      final int argumentIndex) {
    return new ArgumentIsScreamingSnakeCasePredicate(argumentIndex);
  }

  static Predicate<MethodCallExpr> withMethodPreviouslyCalledOnScope(final String methodName) {
    return new MethodPreviouslyCalledOnScope(methodName);
  }

  final class ArgumentCodeContainsPredicate implements Predicate<MethodCallExpr> {

    private final int argumentIndex;
    private final String searchStr;

    ArgumentCodeContainsPredicate(final int argumentIndex, final String searchStr) {
      if (argumentIndex < 0) {
        throw new IllegalArgumentException("must be non-negative");
      }
      this.argumentIndex = argumentIndex;
      this.searchStr = Objects.requireNonNull(searchStr);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      NodeList<Expression> arguments = methodCallExpr.getArguments();
      if (argumentIndex < arguments.size()) {
        Expression argument = methodCallExpr.getArgument(argumentIndex);
        String argumentAsCodeString = argument.toString();
        return argumentAsCodeString != null && argumentAsCodeString.contains(searchStr);
      }
      return false;
    }
  }

  final class ArgumentIsScreamingSnakeCasePredicate implements Predicate<MethodCallExpr> {

    private final int argumentIndex;

    ArgumentIsScreamingSnakeCasePredicate(final int argumentIndex) {
      if (argumentIndex < 0) {
        throw new IllegalArgumentException("must be non-negative");
      }
      this.argumentIndex = argumentIndex;
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      NodeList<Expression> arguments = methodCallExpr.getArguments();
      if (argumentIndex < arguments.size()) {
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

  final class ArgumentNodeTypePredicate implements Predicate<MethodCallExpr> {

    private final Class<? extends Node> type;
    private final int argumentIndex;

    ArgumentNodeTypePredicate(final int argumentIndex, final Class<? extends Node> type) {
      if (argumentIndex < 0) {
        throw new IllegalArgumentException("must be non-negative");
      }
      this.argumentIndex = argumentIndex;
      this.type = Objects.requireNonNull(type);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      NodeList<Expression> arguments = methodCallExpr.getArguments();
      if (argumentIndex < arguments.size()) {
        Expression expression = arguments.get(argumentIndex);
        return this.type.isAssignableFrom(expression.getClass());
      }
      return false;
    }
  }

  final class ArgumentTypePredicate implements Predicate<MethodCallExpr> {

    private final String type;
    private final int argumentIndex;
    private final TypeLocator typeLocator;

    ArgumentTypePredicate(final CompilationUnit cu, final int argumentIndex, final String type) {
      if (argumentIndex < 0) {
        throw new IllegalArgumentException("must be non-negative");
      }
      this.argumentIndex = argumentIndex;
      this.type = Objects.requireNonNull(type);
      this.typeLocator = TypeLocator.createDefault(cu);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      NodeList<Expression> arguments = methodCallExpr.getArguments();
      if (argumentIndex < arguments.size()) {
        Expression expression = arguments.get(argumentIndex);
        String type = typeLocator.locateType(expression);
        return this.type.equals(type);
      }
      return false;
    }
  }

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

  final class MethodPreviouslyCalledOnScope implements Predicate<MethodCallExpr> {

    private final String methodName;

    MethodPreviouslyCalledOnScope(final String methodName) {
      this.methodName = Objects.requireNonNull(methodName);
    }

    @Override
    public boolean test(final MethodCallExpr methodCallExpr) {
      Optional<Expression> scopeRef = methodCallExpr.getScope();
      if (scopeRef.isEmpty()) {
        return false;
      }
      Expression scope = scopeRef.get();
      Optional<MethodDeclaration> methodRef = ASTs.findMethodBodyFrom(methodCallExpr);
      if (methodRef.isEmpty()) {
        return false;
      }
      MethodDeclaration method = methodRef.get();
      var previousMethodCallsOnScope =
          method.findAll(MethodCallExpr.class).stream()
              .filter(
                  expr ->
                      expr.getScope().isPresent() && Objects.equals(expr.getScope().get(), scope))
              .filter(expr -> methodName.equals(expr.getNameAsString()))
              .takeWhile(expr -> expr != methodCallExpr)
              .collect(Collectors.toList());

      return !previousMethodCallsOnScope.isEmpty();
    }
  }

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
      if (scopeRef.isPresent()) {
        Expression scope = scopeRef.get();
        String scopeTypeName = typeLocator.locateType(scope);
        return name.equals(scopeTypeName);
      }
      return false;
    }
  }
}
