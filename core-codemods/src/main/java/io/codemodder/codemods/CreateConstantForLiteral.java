package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.providers.sonar.api.Issue;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A class that extends DefineConstantForLiteral and specializes in creating new constants for
 * string literals in Java code.
 */
final class CreateConstantForLiteral extends DefineConstantForLiteral {

  CreateConstantForLiteral(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {
    super(context, cu, stringLiteralExpr, issue);
  }

  /**
   * Retrieves the suggested constant name from the issue's message. The method utilizes a {@link
   * ConstantNameStringGenerator} to generate a unique constant name based on various factors such
   * as the string literal value, existing names in the CompilationUnit, parent node name, and
   * naming conventions.
   */
  @Override
  protected String getConstantName() {
    final List<FieldDeclaration> constantFieldDeclarations =
        findDeclaredConstantsInClassOrInterface();

    final NodeWithSimpleName<?> nodeWithSimpleName = findAncestorWithSimpleName(stringLiteralExpr);

    final String parentNodeName =
        nodeWithSimpleName != null ? nodeWithSimpleName.getNameAsString() : null;

    return ConstantNameStringGenerator.generateConstantName(
        stringLiteralExpr.getValue(),
        getNamesInCu(),
        parentNodeName,
        isUsingSnakeCase(constantFieldDeclarations));
  }

  /** Retrieves the names of all nodes with simple names in the CompilationUnit. */
  private Set<String> getNamesInCu() {
    return cu.findAll(Node.class).stream()
        .filter(node -> node instanceof NodeWithSimpleName<?>)
        .map(node -> (NodeWithSimpleName<?>) node)
        .map(NodeWithSimpleName::getNameAsString)
        .collect(Collectors.toSet());
  }

  /** Defines a new constant by adding a {@link FieldDeclaration} to the class or interface. */
  @Override
  protected void defineConstant(final String constantName) {
    addConstantFieldToClass(createConstantField(constantName));
  }

  /**
   * Adds a {@link FieldDeclaration} as the last member of the provided {@link
   * ClassOrInterfaceDeclaration}. Adding last seems like it would be <a
   * href="https://github.com/pixee/codemodder-java/issues/288">preferred by users and better for
   * JavaParser to match the existing indentation</a>.
   */
  private void addConstantFieldToClass(final FieldDeclaration constantField) {
    final NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();
    members.addLast(constantField);
  }

  /** Creates a {@link FieldDeclaration} of {@link String} type with the constant name provided */
  private FieldDeclaration createConstantField(final String constantName) {

    final NodeList<Modifier> modifiers =
        NodeList.nodeList(
            Modifier.privateModifier(), Modifier.staticModifier(), Modifier.finalModifier());

    final Type type = new ClassOrInterfaceType(null, "String");

    final VariableDeclarator variableDeclarator =
        new VariableDeclarator(
            type, constantName, new StringLiteralExpr(stringLiteralExpr.getValue()));

    return new FieldDeclaration(modifiers, variableDeclarator);
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
  private List<FieldDeclaration> findDeclaredConstantsInClassOrInterface() {
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

  /**
   * Checks if the first constant field's name contains an underscore or is entirely in uppercase,
   * indicating the use of snake case naming convention.
   */
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
