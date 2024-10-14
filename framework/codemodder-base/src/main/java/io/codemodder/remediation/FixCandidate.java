package io.codemodder.remediation;

import com.github.javaparser.ast.Node;
import java.util.List;
import java.util.Objects;

/**
 * The potential fix location. Associates a relevant node, gathered by the searcher, with the issue
 * of type T.
 */
public record FixCandidate<T>(Node node, List<T> issues) {

  public FixCandidate {
    Objects.requireNonNull(node);
    Objects.requireNonNull(issues);
    if (issues.isEmpty()) {
      throw new IllegalArgumentException("issues cannot be empty");
    }
  }
}
