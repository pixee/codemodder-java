package io.openpixee.java.protections;

import io.codemodder.Weave;
import java.util.Objects;
import java.util.Optional;

/** Represents the results of an AST transformation. */
public final class TransformationResult<T> {

  private final Optional<T> replacementNode;
  private final Weave weave;

  /**
   * @param replacementNode the node that should be returned by the {@link
   *     com.github.javaparser.ast.visitor.ModifierVisitor} event method
   * @param weave the change that was made
   */
  public TransformationResult(final Optional<T> replacementNode, final Weave weave) {
    this.replacementNode = Objects.requireNonNull(replacementNode);
    this.weave = Objects.requireNonNull(weave);
  }

  /**
   * The node that should be returned by the {@link
   * com.github.javaparser.ast.visitor.ModifierVisitor} event method, if any.
   */
  public Optional<T> getReplacementNode() {
    return replacementNode;
  }

  public Weave getWeave() {
    return weave;
  }
}
