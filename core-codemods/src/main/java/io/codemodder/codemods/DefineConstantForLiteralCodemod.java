package io.codemodder.codemods;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Flow;
import io.codemodder.providers.sonar.api.Issue;
import java.util.*;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A codemod for creating a constant for a literal string that is duplicated n times. */
@Codemod(
    id = "sonar:java/define-constant-for-duplicate-literal-s1192",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DefineConstantForLiteralCodemod
    extends SonarPluginJavaParserChanger<StringLiteralExpr> {

  @Inject
  public DefineConstantForLiteralCodemod(
      @ProvidedSonarScan(ruleId = "java:S1192") final RuleIssues issues) {
    super(issues, StringLiteralExpr.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {

    // Validate if we need to define a constant
    if (!issue.getMessage().startsWith("Define a constant")) {
      return false;
    }

    // Validate ClassOrInterfaceDeclaration node where constant will be defined
    final Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional =
        stringLiteralExpr.findAncestor(ClassOrInterfaceDeclaration.class);

    if (classOrInterfaceDeclarationOptional.isEmpty()) {
      return false;
    }

    final int numberOfDuplications = issue.getFlows().size();

    final List<Node> nodesToReplace = findStringLiteralNodesToReplace(context, cu, issue);

    if (nodesToReplace.size() != numberOfDuplications) {
      LOG.debug(
          "Number of duplications {} are not matching nodes to replace {}",
          numberOfDuplications,
          nodesToReplace.size());
    }

    final ClassOrInterfaceDeclaration classOrInterfaceDeclaration =
        classOrInterfaceDeclarationOptional.get();

    final List<FieldDeclaration> constantFieldDeclarations =
        findDeclaredConstantsInClassOrInterface(classOrInterfaceDeclaration);

    final NodeWithSimpleName<?> nodeWithSimpleName = findAncestorWithSimpleName(stringLiteralExpr);

    final String parentNodeName =
        nodeWithSimpleName != null ? nodeWithSimpleName.getNameAsString() : null;

    final VariableCollector variableCollector = new VariableCollector();
    variableCollector.visit(cu, null);

    final String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExpr.getValue(),
            variableCollector.getDeclaredVariables(),
            parentNodeName,
            isUsingSnakeCase(constantFieldDeclarations));

    addConstantFieldToClass(
        classOrInterfaceDeclaration, createConstantField(stringLiteralExpr, constantName));

    nodesToReplace.forEach(
        node -> replaceDuplicatedLiteralToConstantExpression(node, constantName));

    return true;
  }

  /**
   * Finds all reported {@link StringLiteralExpr} nodes by Sonar. It reads source code regions of
   * the Issue's flows to check if region node matches to collect all {@link StringLiteralExpr}
   * nodes to replace.
   */
  private List<Node> findStringLiteralNodesToReplace(
      final CodemodInvocationContext context, final CompilationUnit cu, final Issue issue) {
    final List<? extends Node> allNodes = cu.findAll(StringLiteralExpr.class);

    final List<Node> nodesToReplace = new ArrayList<>();

    for (Flow flow : issue.getFlows()) {
      for (Node node : allNodes) {
        final SourceCodeRegion region =
            createSourceCodeRegion(flow.getLocations().get(0).getTextRange());

        if (!StringLiteralExpr.class.isAssignableFrom(node.getClass())) {
          continue;
        }

        if (context.lineIncludesExcludes().matches(region.start().line())
            && node.getRange().isPresent()) {
          final Range range = node.getRange().get();
          if (RegionNodeMatcher.MATCHES_START.matches(region, range)) {
            nodesToReplace.add(node);
          }
        }
      }
    }

    return nodesToReplace;
  }

  /** Replaces given {@link StringLiteralExpr} to a {@link NameExpr} */
  private void replaceDuplicatedLiteralToConstantExpression(
      final Node node, final String constantName) {
    final StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
    final NameExpr nameExpr = new NameExpr(constantName);
    stringLiteralExpr.replace(nameExpr);
  }

  /**
   * Adds a {@link FieldDeclaration} as the first member of the provided {@link
   * ClassOrInterfaceDeclaration}
   */
  private void addConstantFieldToClass(
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final FieldDeclaration constantField) {

    final NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

    members.addFirst(constantField);
  }

  /** Creates a {@link FieldDeclaration} of {@link String} type with the constant name provided */
  private FieldDeclaration createConstantField(
      final StringLiteralExpr stringLiteralExpr, final String constantName) {

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

  private static final Logger LOG = LoggerFactory.getLogger(DefineConstantForLiteralCodemod.class);
}
