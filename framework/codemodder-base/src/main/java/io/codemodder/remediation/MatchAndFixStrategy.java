package io.codemodder.remediation;

import com.github.javaparser.ast.Node;

/** Provides matching logic as well as a fix strategy */
public abstract class MatchAndFixStrategy implements RemediationStrategy {

  /**
   * Matches a node against an expected pattern for a fix.
   *
   * @param node
   * @return
   */
  public abstract boolean match(final Node node);
}
