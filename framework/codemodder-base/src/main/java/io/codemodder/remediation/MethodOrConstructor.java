package io.codemodder.remediation;

import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import java.util.Collection;
import java.util.Objects;

/** Convenience type to represent a method or constructor call in our APIs that work with both. */
public record MethodOrConstructor(Node node) {

  public MethodOrConstructor {
    Objects.requireNonNull(node);
  }

  /** Return the wrapped node as a {@link Node}. */
  public Node asNode() {
    return this.node;
  }

  /** This assumes a range is present, and explodes if it doesn't. */
  public Range getRange() {
    return this.asNode().getRange().orElseThrow();
  }

  /** Return the wrapped node as a {@link com.github.javaparser.ast.nodeTypes.NodeWithArguments}. */
  public NodeWithArguments<?> asNodeWithArguments() {
    return (NodeWithArguments<?>) this.node;
  }

  /** Get the arguments for the call. */
  public NodeList<?> getArguments() {
    return this.asNodeWithArguments().getArguments();
  }

  /** Return true if this is a constructor call. */
  public boolean isConstructor() {
    return this.node instanceof ObjectCreationExpr;
  }

  /** Return true if this is a method call. */
  public boolean isMethodCall() {
    return this.node instanceof MethodCallExpr;
  }

  /** Return true if this is a method call with a scope. */
  public boolean isMethodCallWithScope() {
    return this.node instanceof MethodCallExpr mce && mce.getScope().isPresent();
  }

  /** Return true if this is a constructor call for the given type. */
  public boolean isConstructorForType(final String type) {
    return this.node instanceof ObjectCreationExpr oce && oce.getTypeAsString().equals(type);
  }

  /** Return true if the node has a range, meaning it was not added by us. */
  public boolean hasRange() {
    return this.asNode().getRange().isPresent();
  }

  /** Return true if this is a method call and it has the given name. */
  public boolean isMethodCallWithName(final String name) {
    return this.isMethodCall() && ((MethodCallExpr) this.node).getNameAsString().equals(name);
  }

  /** Return true if this is a method call and it has one of the given names. */
  public boolean isMethodCallWithNameIn(final Collection<String> names) {
    return this.isMethodCall() && names.contains(((MethodCallExpr) this.node).getNameAsString());
  }

  /** Return the wrapped node as a {@link MethodCallExpr} or blow up. */
  public MethodCallExpr asMethodCall() {
    if (this.isMethodCall()) {
      return (MethodCallExpr) this.node;
    }
    throw new IllegalStateException("Not a method call");
  }

  /** Return the wrapped node as a {@link ObjectCreationExpr} or blow up. */
  public ObjectCreationExpr asObjectCreationExpr() {
    if (this.isConstructor()) {
      return (ObjectCreationExpr) this.node;
    }
    throw new IllegalStateException("Not a constructor");
  }
}
