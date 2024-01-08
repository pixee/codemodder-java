package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class DeclareVariableOnSeparateLineForFieldDeclaration extends DeclareVariableOnSeparateLine {

  private final FieldDeclaration fieldDeclaration;

  DeclareVariableOnSeparateLineForFieldDeclaration(final FieldDeclaration parentNode) {
    super(parentNode);
    this.fieldDeclaration = Objects.requireNonNull(parentNode);
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
   * Adds a list of nodes to the parent node {@link ClassOrInterfaceDeclaration} after the
   * fieldDeclaration.
   */
  protected boolean addNewNodesToParentNode(List<Node> nodesToAdd) {
    final Optional<Node> classOrInterfaceDeclarationOptional = fieldDeclaration.getParentNode();
    if (classOrInterfaceDeclarationOptional.isPresent()
        && classOrInterfaceDeclarationOptional.get()
            instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

      final List<BodyDeclaration<?>> allMembers =
          insertNodesAfterReference(
              classOrInterfaceDeclaration.getMembers().stream().toList(),
              fieldDeclaration,
              nodesToAdd);

      classOrInterfaceDeclaration.setMembers(new NodeList<>(allMembers));
      return true;
    }

    return false;
  }
}
