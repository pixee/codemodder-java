package io.codemodder.remediation;

import java.util.List;
import java.util.Objects;

/** The potential fix location. */
public record FixCandidate<T>(MethodOrConstructor call, List<T> issues) {

  public FixCandidate {
    Objects.requireNonNull(call);
    Objects.requireNonNull(issues);
    if (issues.isEmpty()) {
      throw new IllegalArgumentException("issues cannot be empty");
    }
  }
}
