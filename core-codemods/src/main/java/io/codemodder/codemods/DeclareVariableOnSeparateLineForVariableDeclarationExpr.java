package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeclareVariableOnSeparateLineForVariableDeclarationExpr
    extends DeclareVariableOnSeparateLine {

  private final VariableDeclarationExpr variableDeclarationExpr;

  DeclareVariableOnSeparateLineForVariableDeclarationExpr(final NodeWithVariables<?> parentNode) {
    super(parentNode);
    this.variableDeclarationExpr = (VariableDeclarationExpr) parentNode;
  }

  /**
   * Returns a list of {@link VariableDeclarationExpr} nodes created from a list of {@link
   * VariableDeclarator}s.
   */
  protected List<Node> createVariableNodesToAdd(List<VariableDeclarator> inlineVariables) {
    final List<Node> nodesToAdd = new ArrayList<>();
    for (VariableDeclarator inlineVariable : inlineVariables) {
      final VariableDeclarationExpr newVariableDeclarationExpr =
          new VariableDeclarationExpr(
              variableDeclarationExpr.getModifiers(), new NodeList<>(inlineVariable));
      final ExpressionStmt expressionStmt = new ExpressionStmt(newVariableDeclarationExpr);
      nodesToAdd.add(expressionStmt);
    }
    return nodesToAdd;
  }

  /** Adds a list of nodes to the parent BlockStmt after the expressionStmt. */
  protected void addNewNodesToParentNode(List<Node> nodesToAdd) {
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
}
