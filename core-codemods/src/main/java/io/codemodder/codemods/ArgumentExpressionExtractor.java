package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;
import java.util.stream.Collectors;

final class ArgumentExpressionExtractor {

  private ArgumentExpressionExtractor() {}

  /**
   * Extracts all expressions from the arguments and their child nodes of a MethodCallExpr.
   *
   * @param methodCallExpr The MethodCallExpr to extract expressions from.
   * @return A list containing all expressions found in the MethodCallExpr arguments and their child
   *     nodes.
   */
  static List<Expression> extractExpressions(final MethodCallExpr methodCallExpr) {
    final NodeList<Expression> arguments = methodCallExpr.getArguments();
    return arguments.stream()
        .map(ArgumentExpressionExtractor::getExpressionsFromNode)
        .flatMap(List::stream)
        .toList();
  }

  /**
   * Recursively collects all expressions from a given node and its child nodes.
   *
   * @param node The node to collect expressions from.
   * @return A list containing all expressions found in the node and its child nodes.
   */
  private static List<Expression> getExpressionsFromNode(final Node node) {
    final List<Expression> expressions =
        node.getChildNodes().stream()
            .flatMap(childNode -> getExpressionsFromNode(childNode).stream())
            .collect(Collectors.toList());
    if (node instanceof Expression expression) {
      expressions.add(expression);
    }
    return expressions;
  }
}
