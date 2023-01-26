package io.openpixee.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This is meant to be consumed by rule writers to help them reduce boilerplate and reduce mistakes
 * when writing code to match certain conditions of code to match.
 */
public interface ObjectCreationPredicateFactory {

  static Predicate<ObjectCreationExpr> withType(final String type) {
    return new FullyQualifiedNamePredicate(type);
  }

  static Predicate<ObjectCreationExpr> withArgumentType(
      final CompilationUnit cu, final int argumentIndex, final String name) {
    return new ArgumentTypePredicate(cu, argumentIndex, name);
  }

  static Predicate<ObjectCreationExpr> withArgumentCount(final int argumentCount) {
    return new ArgumentCountPredicate(argumentCount);
  }

  static Predicate<ObjectCreationExpr> withParentType(Class<? extends Node> type) {
    return new ParentNodeTypePredicate(type);
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

  final class ParentNodeTypePredicate implements Predicate<ObjectCreationExpr> {

    private final Class<? extends Node> type;

    ParentNodeTypePredicate(final Class<? extends Node> type) {
      this.type = Objects.requireNonNull(type);
    }

    @Override
    public boolean test(final ObjectCreationExpr objectCreationExpr) {
      if (objectCreationExpr.getParentNode().isPresent()) {
        Node parent = objectCreationExpr.getParentNode().get();
        return this.type.isAssignableFrom(parent.getClass());
      }
      return false;
    }
  }

  final class ArgumentCountPredicate implements Predicate<ObjectCreationExpr> {

    private final int argumentCount;

    private ArgumentCountPredicate(final int argumentCount) {
      if (argumentCount < 0) {
        throw new IllegalArgumentException("must be non-negative");
      }
      this.argumentCount = argumentCount;
    }

    @Override
    public boolean test(final ObjectCreationExpr objectCreationExpr) {
      return objectCreationExpr.getArguments().size() == argumentCount;
    }
  }

  final class ArgumentTypePredicate implements Predicate<ObjectCreationExpr> {

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
    public boolean test(final ObjectCreationExpr objectCreationExpr) {
      NodeList<Expression> arguments = objectCreationExpr.getArguments();
      if (argumentIndex < arguments.size()) {
        Expression expression = arguments.get(argumentIndex);
        String type = typeLocator.locateType(expression);
        return this.type.equals(type);
      }
      return false;
    }
  }
}
