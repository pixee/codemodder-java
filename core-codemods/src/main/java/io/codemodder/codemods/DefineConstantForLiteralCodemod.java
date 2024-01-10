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
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Flow;
import io.codemodder.providers.sonar.api.Issue;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    final NodeWithSimpleName<?> nodeWithSimpleName = findAncestorWithSimpleName(stringLiteralExpr);

    final String parentNodeName =
        nodeWithSimpleName != null ? nodeWithSimpleName.getNameAsString() : null;

    final VariableCollector variableCollector = new VariableCollector();
    variableCollector.visit(cu, null);

    final String constantName =
        ConstantNameGenerator.generateConstantName(
            stringLiteralExpr.getValue(), variableCollector.getDeclaredVariables(), parentNodeName);

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

  /** This class generates constant names based on given values and parent node name. */
  static final class ConstantNameGenerator {

    private ConstantNameGenerator() {}

    /**
     * Generates a unique constant name based on the provided string literal expression value,
     * declared variables, and the name of the parent node (if available). If there's a collision, a
     * suffix counter is added.
     */
    static String generateConstantName(
        final String stringLiteralExprValue,
        final Set<String> declaredVariables,
        final String parentNodeName) {
      final String sanitizedConstantName = formatValue(stringLiteralExprValue, parentNodeName);

      String constantName = sanitizedConstantName;
      int counter = 1;
      while (existsVariable(constantName, declaredVariables)) {
        // If the constant name already exists, append a counter to make it unique
        constantName = sanitizedConstantName;
        if (counter != 0) {
          constantName += "_" + counter;
        }
        counter++;
      }

      return constantName;
    }

    /**
     * Formats the value to be used in the constant name. The process involves removing leading
     * numeric characters, special characters, and spaces from the name, and converting it to
     * uppercase to comply with Java constant naming conventions.
     */
    private static String formatValue(
        final String stringLiteralExprValue, final String parentNodeName) {

      final String constName = buildName(stringLiteralExprValue, parentNodeName);

      final String sanitizedString = sanitizeString(constName);

      final String stringWithoutLeadingNumericCharacters =
          sanitizedString.replaceAll("^\\d*(_)*", "");

      return stringWithoutLeadingNumericCharacters.toUpperCase();
    }

    /**
     * Builds the name to be used in the constant name. It checks if the provided string literal
     * expression value contains only non-alphabetical characters. If it doesn't, the original value
     * is returned as is. Otherwise, the method combines the provided string literal expression
     * value with an optional prefix based on the parent node name (if available) to create a base
     * name.
     */
    private static String buildName(
        final String stringLiteralExprValue, final String parentNodeName) {

      if (!containsOnlyNonAlpha(stringLiteralExprValue)) {
        return stringLiteralExprValue;
      }

      final String prefix = parentNodeName != null ? parentNodeName : "CONST";
      return prefix + " " + stringLiteralExprValue;
    }

    /** Checks if the input contains only non-alpha characters. */
    private static boolean containsOnlyNonAlpha(final String input) {
      // Use a regular expression to check if the string contains only non-alpha characters
      return input.matches("[^a-zA-Z]+");
    }

    /** Sanitizes the input string by keeping only alphanumeric characters and underscores. */
    private static String sanitizeString(final String input) {
      // Use a regular expression to keep only alphanumeric characters and underscores
      final Pattern pattern = Pattern.compile("\\W");
      final Matcher matcher = pattern.matcher(input);

      // Replace non-alphanumeric characters with a single space
      final String stringWithSpaces = matcher.replaceAll(" ");

      // Replace consecutive spaces with a single space
      final String stringWithSingleSpaces = stringWithSpaces.replaceAll("\\s+", " ");

      // Replace spaces with underscores
      return stringWithSingleSpaces.trim().replace(" ", "_");
    }

    /**
     * Checks if a variable with the given constant name already exists in the declared variables
     * set.
     */
    private static boolean existsVariable(
        final String constantName, final Set<String> declaredVariables) {

      if (declaredVariables == null || declaredVariables.isEmpty()) {
        return false;
      }

      return declaredVariables.contains(constantName);
    }
  }

  /** This class collects variables using a visitor pattern. */
  private static final class VariableCollector extends VoidVisitorAdapter<Void> {
    private final Set<String> declaredVariables = new HashSet<>();

    public Set<String> getDeclaredVariables() {
      return declaredVariables;
    }

    @Override
    public void visit(final VariableDeclarator declarator, final Void arg) {
      declaredVariables.add(declarator.getNameAsString());
      super.visit(declarator, arg);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(DefineConstantForLiteralCodemod.class);
}
