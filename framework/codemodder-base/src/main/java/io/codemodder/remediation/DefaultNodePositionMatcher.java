package io.codemodder.remediation;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

class DefaultNodePositionMatcher implements NodePositionMatcher {

  @Override
  public boolean match(final Node node, final int line) {
    return getRange(node).begin.line == line;
  }

  @Override
  public boolean match(final Node node, int startLine, int endLine) {
    return inInterval(getRange(node).begin.line, startLine, endLine);
  }

  @Override
  public boolean match(final Node node, int startLine, int endLine, int startColumn) {
    return match(node, startLine, endLine)
        && getRange(node).begin.compareTo(new Position(startLine, startColumn)) <= 0;
  }

  @Override
  public boolean match(
      final Node node, int startLine, int endLine, int startColumn, int endColumn) {
    return getRange(node)
        .strictlyContains(
            new Range(new Position(startLine, startColumn), new Position(endLine, endColumn)));
  }

  private boolean inInterval(int number, int upper, int lower) {
    return number >= upper && number <= lower;
  }

  Range getRange(final Node node) {
    return node.getRange().orElseThrow();
  }
}
