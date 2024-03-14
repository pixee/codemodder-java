package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.ArrayList;
import java.util.List;

public class ArgumentExpressionExtractor {

  private ArgumentExpressionExtractor() {}

  /**
   * Extracts all expressions from the arguments and their child nodes of a MethodCallExpr.
   *
   * @param methodCallExpr The MethodCallExpr to extract expressions from.
   * @return A list containing all expressions found in the MethodCallExpr arguments and their child
   *     nodes.
   */
  public static List<Expression> extractExpressions(MethodCallExpr methodCallExpr) {
    List<Expression> expressions = new ArrayList<>();
    NodeList<Expression> arguments = methodCallExpr.getArguments();
    for (Expression argument : arguments) {
      expressions.addAll(getExpressionsFromNode(argument));
    }
    return expressions;
  }

  /**
   * Recursively collects all expressions from a given node and its child nodes.
   *
   * @param node The node to collect expressions from.
   * @return A list containing all expressions found in the node and its child nodes.
   */
  private static List<Expression> getExpressionsFromNode(Node node) {
    List<Expression> expressions = new ArrayList<>();
    if (node instanceof Expression expression) {
      expressions.add(expression);
    }
    node.getChildNodes()
        .forEach(childNode -> expressions.addAll(getExpressionsFromNode(childNode)));
    return expressions;
  }
}
