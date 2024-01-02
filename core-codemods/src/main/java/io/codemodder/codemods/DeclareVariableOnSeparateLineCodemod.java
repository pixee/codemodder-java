package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for declaring a variable on a separate line. */
@Codemod(
    id = "sonar:java/declare-variable-on-separate-line-s1659",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DeclareVariableOnSeparateLineCodemod
    extends SonarPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public DeclareVariableOnSeparateLineCodemod(
      @ProvidedSonarScan(ruleId = "java:S1659") final RuleIssues issues) {
    super(issues, VariableDeclarator.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Issue issue) {

    final Optional<Node> parentOptional = variableDeclarator.getParentNode();

    if (parentOptional.isEmpty()) {
      return false;
    }

    final NodeWithVariables<?> parentNode = (NodeWithVariables<?>) parentOptional.get();

    final boolean isFieldDeclaration = parentNode instanceof FieldDeclaration;

    final List<VariableDeclarator> inlineVariables = parentNode.getVariables().stream().toList();

    final NodeList<Modifier> modifiers = getModifiers(parentNode, isFieldDeclaration);

    final NodeList<AnnotationExpr> annotations =
        isFieldDeclaration ? ((FieldDeclaration) parentNode).getAnnotations() : null;

    final List<VariableDeclarator> remainingVariables =
        inlineVariables.subList(1, inlineVariables.size());
    final List<Node> newVariableNodes =
        createVariableNodesToAdd(isFieldDeclaration, remainingVariables, modifiers, annotations);

    if (isFieldDeclaration) {
      addVariablesAsMembersToClassOrInterface(parentNode, newVariableNodes);
    } else {
      addVariablesAsStatementsToBlockStatement(parentNode, newVariableNodes);
    }

    // Replace parent's node all inline variables with only the first inline variable
    parentNode.setVariables(new NodeList<>(inlineVariables.get(0)));

    return true;
  }

  private NodeList<Modifier> getModifiers(
      final NodeWithVariables<?> parentNode, final boolean isFieldDeclaration) {
    return isFieldDeclaration
        ? ((FieldDeclaration) parentNode).getModifiers()
        : ((VariableDeclarationExpr) parentNode).getModifiers();
  }

  // Create remaining variables list. Node type will depend on isFieldDeclaration flag
  private List<Node> createVariableNodesToAdd(
      final boolean isFieldDeclaration,
      final List<VariableDeclarator> inlineVariables,
      final NodeList<Modifier> modifiers,
      final NodeList<AnnotationExpr> annotations) {
    final List<Node> nodesToAdd = new ArrayList<>();
    for (VariableDeclarator inlineVariable : inlineVariables) {
      if (isFieldDeclaration) {
        final FieldDeclaration fieldDeclaration =
            new FieldDeclaration(modifiers, annotations, new NodeList<>(inlineVariable));
        nodesToAdd.add(fieldDeclaration);
      } else {
        final VariableDeclarationExpr variableDeclarationExpr =
            new VariableDeclarationExpr(modifiers, new NodeList<>(inlineVariable));
        final ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
        nodesToAdd.add(expressionStmt);
      }
    }
    return nodesToAdd;
  }

  private void addVariablesAsMembersToClassOrInterface(
      final NodeWithVariables<?> parentNode, final List<Node> nodesToAdd) {
    final FieldDeclaration fieldDeclaration = (FieldDeclaration) parentNode;
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

  private void addVariablesAsStatementsToBlockStatement(
      final NodeWithVariables<?> parentNode, final List<Node> nodesToAdd) {
    final VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) parentNode;
    final Optional<Node> expressionStmtOptional = variableDeclarationExpr.getParentNode();
    if (expressionStmtOptional.isPresent()
        && expressionStmtOptional.get() instanceof ExpressionStmt expressionStmt) {
      final Optional<Node> blockStmtOptional = expressionStmt.getParentNode();
      if (blockStmtOptional.isPresent() && blockStmtOptional.get() instanceof BlockStmt blockStmt) {

        final List<Statement> allStmts =
            NodesInserter.mergeNodes(blockStmt.getStatements(), expressionStmt, nodesToAdd);

        blockStmt.setStatements(new NodeList<>(allStmts));
      }
    }
  }

  private static final class NodesInserter {

    /**
     * Inserts a list of nodes after a specified reference node within an original list of nodes.
     * The result of this operation is a new list containing the original nodes with the additional nodes
     * inserted after the specified reference node while maintaining the original order of elements.
     */
    static <T> List<T> mergeNodes(
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
}
