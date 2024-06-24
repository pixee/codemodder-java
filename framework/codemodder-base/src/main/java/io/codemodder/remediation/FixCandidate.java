package io.codemodder.remediation;

import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Objects;

/** The potential fix location. */
public record FixCandidate<T>(MethodCallExpr methodCall, T issue) {

  public FixCandidate {
    Objects.requireNonNull(methodCall);
    Objects.requireNonNull(issue);
  }
}
