package io.codemodder.codemods;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
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

      if(!issue.getMessage().startsWith("Define a constant")){
          return false;
      }

    final Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional =
        stringLiteralExpr.findAncestor(ClassOrInterfaceDeclaration.class);

    if (classOrInterfaceDeclarationOptional.isEmpty()) {
      return false;
    }

    final int numberOfDuplications = issue.getFlows().size();

    final List<Node> nodesToReplace = findLiteralNodesToReplace(context, cu, issue);

    if (nodesToReplace.size() != numberOfDuplications) {
      return false;
    }

    final ClassOrInterfaceDeclaration classOrInterfaceDeclaration =
        classOrInterfaceDeclarationOptional.get();

      // Collect declared variables using a visitor
      final VariableCollector variableCollector = new VariableCollector();
      variableCollector.visit(cu, null);

    final String constantName = ConstantNameGenerator.buildConstantName(stringLiteralExpr.getValue(), variableCollector.getDeclaredVariables());

    addConstantFieldToClass(classOrInterfaceDeclaration, createConstantField(stringLiteralExpr, constantName));

    nodesToReplace.forEach(node -> replaceDuplicatedLiteralToConstantExpression(node, constantName));

    return true;
  }

  private List<Node> findLiteralNodesToReplace(
      final CodemodInvocationContext context, final CompilationUnit cu, final Issue issue) {
    List<? extends Node> allNodes = cu.findAll(StringLiteralExpr.class);

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
          Range range = node.getRange().get();
          if (RegionNodeMatcher.MATCHES_START.matches(region, range)) {
            nodesToReplace.add(node);
          }
        }
      }
    }

    return nodesToReplace;
  }

  private void replaceDuplicatedLiteralToConstantExpression(final Node node, final String constantName) {
    StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
    NameExpr nameExpr = new NameExpr(constantName);
    stringLiteralExpr.replace(nameExpr);
  }

  private void addConstantFieldToClass(
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final FieldDeclaration constantField) {

    final NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

    members.addFirst(constantField);
  }

  private FieldDeclaration createConstantField(final StringLiteralExpr stringLiteralExpr, final String constantName) {

    final NodeList<Modifier> modifiers =
        NodeList.nodeList(
            Modifier.privateModifier(), Modifier.staticModifier(), Modifier.finalModifier());

    final Type type = new ClassOrInterfaceType(null, "String");

    final VariableDeclarator variableDeclarator = new VariableDeclarator();
    variableDeclarator.setInitializer(new StringLiteralExpr(stringLiteralExpr.getValue()));
    variableDeclarator.setType(type);
    variableDeclarator.setName(constantName);

    return new FieldDeclaration(modifiers, variableDeclarator);
  }



  static final class ConstantNameGenerator{

      private ConstantNameGenerator() {}

      static String buildConstantName(String stringLiteralExprValue, Set<String> declaredVariables) {
          String originalValue = stringLiteralExprValue;
          String sanitizedConstantName = sanitizeString(originalValue);

          String constantName = sanitizedConstantName;
          int counter = 1;
          while (existsVariable(constantName, declaredVariables)) {
              // If the constant name already exists, append a counter to make it unique
              constantName = sanitizedConstantName;
              if(counter != 0){
                  constantName += "_" + counter;
              }
              counter++;
          }

          return constantName;
      }
      private static String sanitizeString(String input) {
          // Use a regular expression to keep only alphanumeric characters and underscores
          Pattern pattern = Pattern.compile("\\W");
          Matcher matcher = pattern.matcher(input);

          // Replace non-alphanumeric characters with a single space
          String stringWithSpaces = matcher.replaceAll(" ");

          // Replace consecutive spaces with a single space
          String stringWithSingleSpaces = stringWithSpaces.replaceAll("\\s+", " ");

          // Replace spaces with underscores
          return stringWithSingleSpaces.trim().replace(" ", "_").toUpperCase();
      }

      private static boolean existsVariable(final String constantName, final Set<String> declaredVariables){

          if(declaredVariables == null || declaredVariables.isEmpty()){
              return false;
          }

          return declaredVariables.contains(constantName);
      }
  }

    private static final class VariableCollector extends VoidVisitorAdapter<Void> {
        private final Set<String> declaredVariables = new HashSet<>();

        public Set<String> getDeclaredVariables() {
            return declaredVariables;
        }

        @Override
        public void visit(VariableDeclarator declarator, Void arg) {
            declaredVariables.add(declarator.getNameAsString());
            super.visit(declarator, arg);
        }
    }
}
