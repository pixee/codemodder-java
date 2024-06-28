package io.codemodder.remediation;

import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.List;
import java.util.Objects;

/** The potential fix location. */
public record FixCandidate<T>(MethodCallExpr methodCall, List<T> issues) {

  public FixCandidate {
    Objects.requireNonNull(methodCall);
    Objects.requireNonNull(issues);
    if (issues.isEmpty()) {
      throw new IllegalArgumentException("issues cannot be empty");
    }
  }
}
