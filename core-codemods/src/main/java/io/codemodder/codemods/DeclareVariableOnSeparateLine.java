package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class DeclareVariableOnSeparateLine {

  protected final NodeWithVariables<?> parentNode;

  protected DeclareVariableOnSeparateLine(final NodeWithVariables<?> parentNode) {
    this.parentNode = Objects.requireNonNull(parentNode);
  }

  /**
   * Splits multiple inline variables within a parent node into separate variable declaration
   * statements.
   */
  protected boolean splitVariablesIntoTheirOwnStatements() {

    final List<VariableDeclarator> inlineVariables = parentNode.getVariables().stream().toList();

    final List<VariableDeclarator> remainingVariables =
        inlineVariables.subList(1, inlineVariables.size());

    final List<Node> newVariableNodes = createVariableNodesToAdd(remainingVariables);

    if (!addNewNodesToParentNode(newVariableNodes)) {
      return false;
    }
    // Replace parent's node that has all inline variables with only the first variable
    parentNode.setVariables(new NodeList<>(inlineVariables.get(0)));

    return true;
  }

  /** Returns a list of nodes created from the list of inline variables. */
  protected abstract List<Node> createVariableNodesToAdd(List<VariableDeclarator> inlineVariables);

  /** Adds new variable nodes to the parent node. */
  protected abstract boolean addNewNodesToParentNode(List<Node> nodesToAdd);

  /**
   * Inserts a list of nodes after a specified reference node within an original list of nodes. The
   * result of this operation is a new list containing the original nodes with the additional nodes
   * inserted after the specified reference node while maintaining the original order of elements.
   */
  static <T> List<T> insertNodesAfterReference(
      final List<T> originalNodes, final Node referenceNode, final List<Node> nodesToAdd) {

    // Find the index of the reference node in the original list
    final int referenceIndex = originalNodes.indexOf(referenceNode);

    // Split the original list into elements before and after the reference node
    final List<T> elementsBefore = originalNodes.subList(0, referenceIndex + 1);
    final List<T> elementsAfter = originalNodes.subList(referenceIndex + 1, originalNodes.size());

    // Create a new list with nodes inserted after the reference node
    final List<T> newElements = new ArrayList<>(elementsBefore);
    nodesToAdd.forEach(node -> newElements.add((T) node));
    newElements.addAll(elementsAfter);

    return newElements;
  }
}
