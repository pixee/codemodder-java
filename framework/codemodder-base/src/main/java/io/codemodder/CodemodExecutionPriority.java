package io.codemodder;

import java.util.Comparator;

/**
 * A description of how important it is that a codemod execute in the list of codemods being run.
 */
public enum CodemodExecutionPriority {
  LOW(0),
  NORMAL(1),
  HIGH(2);

  private final int level;

  CodemodExecutionPriority(final int level) {
    this.level = level;
  }

  /**
   * Expose a package-protected comparator so nobody else needs to worry about how our priority is
   * actually established.
   */
  static final Comparator<CodemodExecutionPriority> priorityOrderComparator =
      (p1, p2) -> Integer.compare(p2.level, p1.level);
}
