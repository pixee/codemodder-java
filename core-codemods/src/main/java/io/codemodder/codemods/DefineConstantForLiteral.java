package io.codemodder.codemods;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Position;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.SourceCodeRegion;
import io.codemodder.providers.sonar.api.Flow;
import io.codemodder.providers.sonar.api.Issue;
import io.codemodder.providers.sonar.api.TextRange;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DefineConstantForLiteral {

  protected ClassOrInterfaceDeclaration classOrInterfaceDeclaration;

  protected final CodemodInvocationContext context;
  protected final CompilationUnit cu;
  protected final StringLiteralExpr stringLiteralExpr;
  protected final Issue issue;

  DefineConstantForLiteral(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {
    this.context = context;
    this.cu = cu;
    this.stringLiteralExpr = stringLiteralExpr;
    this.issue = issue;
  }

  boolean execute() {

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

    defineConstant(constantName);

    nodesToReplace.forEach(
        node -> replaceDuplicatedLiteralToConstantExpression(node, constantName));

    return true;
  }

  protected abstract String getConstantName();

  protected abstract void defineConstant(final String constantName);

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
        final TextRange textRange = flow.getLocations().get(0).getTextRange();
        final Position start =
            new Position(textRange.getStartLine(), textRange.getStartOffset() + 1);
        final Position end = new Position(textRange.getEndLine(), textRange.getEndOffset() + 1);
        final SourceCodeRegion region = new SourceCodeRegion(start, end);

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

  /** Replaces given {@link StringLiteralExpr} to a {@link NameExpr} */
  private void replaceDuplicatedLiteralToConstantExpression(
      final Node node, final String constantName) {
    final StringLiteralExpr literalExpr = (StringLiteralExpr) node;
    final NameExpr nameExpr = new NameExpr(constantName);
    literalExpr.replace(nameExpr);
  }

  private static final Logger LOG = LoggerFactory.getLogger(DefineConstantForLiteral.class);
}