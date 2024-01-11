package io.codemodder.codemods;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.SourceCodeRegion;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Flow;
import io.codemodder.providers.sonar.api.Issue;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DefineConstantForLiteral {

    protected ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

  protected  final CodemodInvocationContext context;
  protected  final CompilationUnit cu;
    protected  final StringLiteralExpr stringLiteralExpr;
    protected  final Issue issue;

  DefineConstantForLiteral(final CodemodInvocationContext context,
                           final CompilationUnit cu,
                           final StringLiteralExpr stringLiteralExpr,
                           final Issue issue){
      this.context = context;
      this.cu = cu;
      this.stringLiteralExpr = stringLiteralExpr;
      this.issue = issue;
  }

  boolean defineConstant() {

    // Validate ClassOrInterfaceDeclaration node where constant will be defined
    final Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional =
        stringLiteralExpr.findAncestor(ClassOrInterfaceDeclaration.class);

    if (classOrInterfaceDeclarationOptional.isEmpty()) {
      return false;
    }

    final int numberOfDuplications = issue.getFlows().size();

    final List<Node> nodesToReplace = findStringLiteralNodesToReplace();

    if (nodesToReplace.size() != numberOfDuplications) {
      LOG.debug(
          "Number of duplications {} are not matching nodes to replace {}",
          numberOfDuplications,
          nodesToReplace.size());
    }

    classOrInterfaceDeclaration = classOrInterfaceDeclarationOptional.get();

    final String constantName = getConstantName();

    addConstantFieldToClass(createConstantField(stringLiteralExpr, constantName));

    nodesToReplace.forEach(
        node -> replaceDuplicatedLiteralToConstantExpression(node, constantName));

    return true;
  }

  protected abstract String getConstantName();

  /**
   * Finds all reported {@link StringLiteralExpr} nodes by Sonar. It reads source code regions of
   * the Issue's flows to check if region node matches to collect all {@link StringLiteralExpr}
   * nodes to replace.
   */
  private List<Node> findStringLiteralNodesToReplace() {
    final List<? extends Node> allNodes = cu.findAll(StringLiteralExpr.class);

    final List<Node> matchingNodes = new ArrayList<>();
    matchingNodes.add(stringLiteralExpr);

    for (Flow flow : issue.getFlows()) {
      for (Node node : allNodes) {
        final SourceCodeRegion region =
            SonarPluginJavaParserChanger.createSourceCodeRegion(
                flow.getLocations().get(0).getTextRange());

        if (!StringLiteralExpr.class.isAssignableFrom(node.getClass())) {
          continue;
        }

        if (context.lineIncludesExcludes().matches(region.start().line())
            && node.getRange().isPresent()) {
          final Range range = node.getRange().get();
          if (RegionNodeMatcher.MATCHES_START.matches(region, range)) {
            matchingNodes.add(node);
          }
        }
      }
    }

    return matchingNodes;
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
   * Adds a {@link FieldDeclaration} as the first member of the provided {@link
   * ClassOrInterfaceDeclaration}
   */
  protected void addConstantFieldToClass(
      final FieldDeclaration constantField) {

    final NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

    members.addFirst(constantField);
  }

  /** Replaces given {@link StringLiteralExpr} to a {@link NameExpr} */
  private void replaceDuplicatedLiteralToConstantExpression(
      final Node node, final String constantName) {
    final StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) node;
    final NameExpr nameExpr = new NameExpr(constantName);
    stringLiteralExpr.replace(nameExpr);
  }

  private static final Logger LOG = LoggerFactory.getLogger(DefineConstantForLiteral.class);
}
