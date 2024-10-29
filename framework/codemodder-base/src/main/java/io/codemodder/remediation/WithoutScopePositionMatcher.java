package io.codemodder.remediation;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithTraversableScope;

/** Removes the range of the node's scope before matching it against a position. */
public final class WithoutScopePositionMatcher extends DefaultNodePositionMatcher {

  @Override
  protected Range getRange(final Node node) {
    var originalRange = node.getRange().orElseThrow();
    if (node.hasScope()) {
      var scope = ((NodeWithTraversableScope) node).traverseScope().get();
      var scopeRange = scope.getRange().orElseThrow();
      return new Range(
          new Position(scopeRange.end.line, scopeRange.end.column + 1), originalRange.end);
    }
    return originalRange;
  }
}
