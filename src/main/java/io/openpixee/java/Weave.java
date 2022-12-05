package io.openpixee.java;

import java.util.List;
import java.util.Objects;

/** Represents a change made to the code. */
public final class Weave {

  /**
   * A human-readable-but-not-necessarily-descriptive string that describes the change, e.g.,
   * "harden-xml-factory".
   */
  private final String changeCode;

  /** Represents the original line number where this change was found to be applicable. */
  private final int lineNumber;

  /** Represents the dependencies required by the new code we introduced in this weave. */
  private final List<DependencyGAV> dependenciesNeeded;

  private Weave(
      final int lineNumber, final String changeCode, final List<DependencyGAV> dependenciesNeeded) {
    this.lineNumber = lineNumber;
    this.changeCode = Objects.requireNonNull(changeCode, "changeCode");
    this.dependenciesNeeded = Objects.requireNonNull(dependenciesNeeded, "dependenciesNeeded");
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Weave weave = (Weave) o;
    return lineNumber == weave.lineNumber
        && changeCode.equals(weave.changeCode)
        && dependenciesNeeded.equals(weave.dependenciesNeeded);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeCode, lineNumber, dependenciesNeeded);
  }

  public int lineNumber() {
    return lineNumber;
  }

  public String changeCode() {
    return changeCode;
  }

  public List<DependencyGAV> getDependenciesNeeded() {
    return dependenciesNeeded;
  }

  /** Builds a weave. */
  public static Weave from(
      final int line, final String code, final List<DependencyGAV> dependenciesNeeded) {
    return new Weave(line, code, dependenciesNeeded);
  }

  /**
   * A {@link Weave} convenience builder for weaves that only requires one new dependency.
   * Equivalent to calling: {@code Weave.from(line, code, List.of(dependencyNeeded))}
   */
  public static Weave from(
      final int line, final String code, final DependencyGAV dependencyNeeded) {
    return new Weave(line, code, List.of(dependencyNeeded));
  }

  /**
   * A {@link Weave} convenience builder for weaves that don't require a new dependency. Equivalent
   * to calling: {@code Weave.from(line, code, List.of())}
   */
  public static Weave from(final int line, final String code) {
    return new Weave(line, code, List.of());
  }

  @Override
  public String toString() {
    return "Weave{"
        + "changeCode='"
        + changeCode
        + '\''
        + ", lineNumber="
        + lineNumber
        + ", dependenciesNeeded="
        + dependenciesNeeded
        + '}';
  }
}
