package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeclareVariableOnSeparateLineForFieldDeclaration
    extends DeclareVariableOnSeparateLine {

  private final FieldDeclaration fieldDeclaration;

  DeclareVariableOnSeparateLineForFieldDeclaration(final NodeWithVariables<?> parentNode) {
    super(parentNode);
    this.fieldDeclaration = (FieldDeclaration) parentNode;
  }

  /**
   * Returns a list of {@link FieldDeclaration} nodes created from a list of {@link
   * VariableDeclarator}s.
   */
  protected List<Node> createVariableNodesToAdd(List<VariableDeclarator> inlineVariables) {
    final List<Node> nodesToAdd = new ArrayList<>();
    for (VariableDeclarator inlineVariable : inlineVariables) {

      final FieldDeclaration newFieldDeclaration =
          new FieldDeclaration(
              fieldDeclaration.getModifiers(),
              ((FieldDeclaration) parentNode).getAnnotations(),
              new NodeList<>(inlineVariable));
      nodesToAdd.add(newFieldDeclaration);
    }
    return nodesToAdd;
  }

  /**
   * Adds a list of nodes to the parent node (ClassOrInterfaceDeclaration) after the
   * fieldDeclaration.
   */
  protected void addNewNodesToParentNode(List<Node> nodesToAdd) {
    final Optional<Node> classOrInterfaceDeclarationOptional = fieldDeclaration.getParentNode();
    if (classOrInterfaceDeclarationOptional.isPresent()
        && classOrInterfaceDeclarationOptional.get()
            instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

      final List<BodyDeclaration<?>> allMembers =
          NodesInserter.mergeNodes(
              classOrInterfaceDeclaration.getMembers().stream().toList(),
              fieldDeclaration,
              nodesToAdd);

      classOrInterfaceDeclaration.setMembers(new NodeList<>(allMembers));
    }
  }
}
