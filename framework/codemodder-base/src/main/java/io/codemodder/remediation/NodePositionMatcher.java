package io.codemodder.remediation;

import com.github.javaparser.ast.Node;

public interface NodePositionMatcher {

  static final NodePositionMatcher DEFAULT = new DefaultNodePositionMatcher();

  /**
   * Matches a node with a range against a line
   *
   * @param node
   * @param line
   * @return
   */
  boolean match(Node node, int line);

  /**
   * Matches a node with a range against a line range
   *
   * @param node
   * @param startLine
   * @param endLine
   * @return
   */
  boolean match(Node node, int startLine, int endLine);

  /**
   * Matches a node with a range against a line range and column
   *
   * @param node
   * @param startLine
   * @param endLine
   * @param StartColumn
   * @return
   */
  boolean match(Node node, int startLine, int endLine, int StartColumn);

  /**
   * Matches a node with a range against another range
   *
   * @param node
   * @param startLine
   * @param endLine
   * @param StartColumn
   * @param EndColumn
   * @return
   */
  boolean match(Node node, int startLine, int endLine, int StartColumn, int EndColumn);
}
