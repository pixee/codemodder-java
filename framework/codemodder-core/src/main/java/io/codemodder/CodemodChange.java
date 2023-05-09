package io.codemodder;

import java.util.List;
import java.util.Objects;

/** Represents a change made to the code. */
public final class CodemodChange {

  /** Represents the original line number where this change was found to be applicable. */
  private final int lineNumber;

  /** Represents the dependencies required by the new code we introduced in this weave. */
  private final List<DependencyGAV> dependenciesNeeded;

  private CodemodChange(final int lineNumber, final List<DependencyGAV> dependenciesNeeded) {
    this.lineNumber = lineNumber;
    this.dependenciesNeeded = Objects.requireNonNull(dependenciesNeeded, "dependenciesNeeded");
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CodemodChange weave = (CodemodChange) o;
    return lineNumber == weave.lineNumber && dependenciesNeeded.equals(weave.dependenciesNeeded);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, dependenciesNeeded);
  }

  public int lineNumber() {
    return lineNumber;
  }

  public List<DependencyGAV> getDependenciesNeeded() {
    return dependenciesNeeded;
  }

  /** Builds a weave. */
  public static CodemodChange from(final int line, final List<DependencyGAV> dependenciesNeeded) {
    return new CodemodChange(line, dependenciesNeeded);
  }

  /**
   * A {@link CodemodChange} convenience builder for weaves that only requires one new dependency.
   * Equivalent to calling: {@code CodemodChange.from(line, code, List.of(dependencyNeeded))}
   */
  public static CodemodChange from(final int line, final DependencyGAV dependencyNeeded) {
    return new CodemodChange(line, List.of(dependencyNeeded));
  }

  /**
   * A {@link CodemodChange} convenience builder for weaves that don't require a new dependency.
   * Equivalent to calling: {@code CodemodChange.from(line, code, List.of())}
   */
  public static CodemodChange from(final int line) {
    return new CodemodChange(line, List.of());
  }

  @Override
  public String toString() {
    return "CodemodChange{"
        + "lineNumber="
        + lineNumber
        + ", dependenciesNeeded="
        + dependenciesNeeded
        + '}';
  }
}
