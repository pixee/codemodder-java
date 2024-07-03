package io.codemodder.remediation.xss;

import java.util.List;
import java.util.Objects;

/** A collection of issues that are all fixable at the same line (and column, if available). */
record XSSFixGroup<T>(List<T> issues) {
  XSSFixGroup {
    Objects.requireNonNull(issues, "issues");
    if (issues.isEmpty()) {
      throw new IllegalArgumentException("Must have at least one issue");
    }
  }
}
