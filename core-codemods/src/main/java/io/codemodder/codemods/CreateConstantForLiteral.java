package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.List;
import java.util.Optional;

public class CreateConstantForLiteral extends DefineConstantForLiteral {

  @Override
  protected String getConstantName(
      final StringLiteralExpr stringLiteralExpr, final CompilationUnit cu) {
    final List<FieldDeclaration> constantFieldDeclarations =
        findDeclaredConstantsInClassOrInterface(classOrInterfaceDeclaration);

    final NodeWithSimpleName<?> nodeWithSimpleName = findAncestorWithSimpleName(stringLiteralExpr);

    final String parentNodeName =
        nodeWithSimpleName != null ? nodeWithSimpleName.getNameAsString() : null;

    final VariableCollector variableCollector = new VariableCollector();
    variableCollector.visit(cu, null);

    return ConstantNameStringGenerator.generateConstantName(
        stringLiteralExpr.getValue(),
        variableCollector.getDeclaredVariables(),
        parentNodeName,
        isUsingSnakeCase(constantFieldDeclarations));
  }

  /**
   * Retrieves the first ancestor node that is a {@link NodeWithSimpleName} of a {@link
   * StringLiteralExpr}
   */
  private NodeWithSimpleName<?> findAncestorWithSimpleName(
      final StringLiteralExpr stringLiteralExpr) {
    Optional<Node> parentNodeOptional = stringLiteralExpr.getParentNode();

    while (parentNodeOptional.isPresent()
        && !(parentNodeOptional.get() instanceof NodeWithSimpleName)) {
      parentNodeOptional = parentNodeOptional.get().getParentNode();
    }

    return (NodeWithSimpleName<?>) parentNodeOptional.orElse(null);
  }

  /**
   * This method takes a {@link ClassOrInterfaceDeclaration} as input, filters its members to
   * include only {@link FieldDeclaration} nodes, and further filters these FieldDeclarations to
   * select those that are declared as static, final, and have a type of String. The resulting list
   * represents the declared constants in the given class or interface.
   */
  private List<FieldDeclaration> findDeclaredConstantsInClassOrInterface(
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
    return classOrInterfaceDeclaration.getMembers().stream()
        .filter(FieldDeclaration.class::isInstance)
        .map(FieldDeclaration.class::cast)
        .filter(
            fieldDeclaration ->
                fieldDeclaration.getModifiers().contains(Modifier.staticModifier())
                    && fieldDeclaration.getModifiers().contains(Modifier.finalModifier())
                    && containsStringType(fieldDeclaration))
        .toList();
  }

  /**
   * This method takes a list of {@link FieldDeclaration} objects representing constant fields. It
   * checks if the first constant field's name contains an underscore or is entirely in uppercase,
   * indicating the use of snake case naming convention. If the list is empty, the method returns
   * true as there are no constant fields to assess.
   */
  private boolean isUsingSnakeCase(final List<FieldDeclaration> constantFieldDeclarations) {
    if (constantFieldDeclarations == null || constantFieldDeclarations.isEmpty()) {
      return true;
    }

    final String constantName = constantFieldDeclarations.get(0).getVariable(0).getNameAsString();

    return constantName.contains("_") || constantName.equals(constantName.toUpperCase());
  }

  private boolean containsStringType(final FieldDeclaration fieldDeclaration) {
    for (VariableDeclarator variable : fieldDeclaration.getVariables()) {
      final Type fieldType = variable.getType();
      if (fieldType instanceof ClassOrInterfaceType classOrInterfaceType
          && classOrInterfaceType.getNameAsString().equals("String")) {
        return true;
      }
    }
    return false;
  }
}
