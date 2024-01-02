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
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Flow;
import io.codemodder.providers.sonar.api.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    addConstantFieldToClass(classOrInterfaceDeclaration, createConstantField(stringLiteralExpr));

    nodesToReplace.forEach(this::replaceDuplicatedLiteralToConstantExpression);

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

  private void replaceDuplicatedLiteralToConstantExpression(final Node node) {
    StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
    NameExpr nameExpr = new NameExpr(buildConstantName(stringLiteralExpr));
    stringLiteralExpr.replace(nameExpr);
  }

  private void addConstantFieldToClass(
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final FieldDeclaration constantField) {

    final NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

    members.addFirst(constantField);
  }

  private FieldDeclaration createConstantField(final StringLiteralExpr stringLiteralExpr) {
    final String constantName = buildConstantName(stringLiteralExpr);

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

  private String buildConstantName(final StringLiteralExpr stringLiteralExpr) {
    return stringLiteralExpr.getValue().toUpperCase();
  }
}
