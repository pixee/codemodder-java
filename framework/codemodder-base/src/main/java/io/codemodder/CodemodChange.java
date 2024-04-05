package io.codemodder;

import io.codemodder.codetf.CodeTFParameter;
import io.codemodder.codetf.FixedFinding;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/** Represents a change made to the code. */
public final class CodemodChange {

  /** Represents the original line number where this change was found to be applicable. */
  private final int lineNumber;

  /** Represents the dependencies required by the new code we introduced in this weave. */
  private final List<DependencyGAV> dependenciesNeeded;

  /**
   * The fixed finding that was addressed by this change.
   */
  private final FixedFinding fixedFinding;

  /**
   * Represents the parameters that were used to generate the new code we introduced in this weave.
   */
  private final List<CodeTFParameter> parameters;

  /** Represents a description for this change to be used instead of the standard static text. */
  private @Nullable final String description;

  private CodemodChange(final int lineNumber, final List<DependencyGAV> dependenciesNeeded) {
    this.lineNumber = lineNumber;
    this.dependenciesNeeded = Objects.requireNonNull(dependenciesNeeded, "dependenciesNeeded");
    this.parameters = List.of();
    this.description = null;
    this.fixedFinding = null;
  }

  private CodemodChange(final int lineNumber, final FixedFinding finding) {
    this.lineNumber = lineNumber;
    this.dependenciesNeeded = List.of();
    this.parameters = List.of();
    this.description = null;
    this.fixedFinding = Objects.requireNonNull(finding);
  }

  private CodemodChange(final int lineNumber, final String description) {
    this.lineNumber = lineNumber;
    this.dependenciesNeeded = List.of();
    this.parameters = List.of();
    this.description = Objects.requireNonNull(description);
    this.fixedFinding = null;
  }

  private CodemodChange(final int lineNumber, final Parameter parameter, final String valueUsed) {
    this.lineNumber = lineNumber;
    this.dependenciesNeeded = List.of();
    CodeTFParameter codeTFParameter =
        new CodeTFParameter(
            parameter.getQuestion(),
            parameter.getName(),
            parameter.getType(),
            parameter.getLabel(),
            valueUsed);
    this.parameters = List.of(codeTFParameter);
    this.description = null;
    this.fixedFinding = null;
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

  /**
   * The line number associated with the change. Doesn't necessarily mean the line where it starts,
   * ends, or was discovered, but should be deterministic on consecutive runs.
   */
  public int lineNumber() {
    return lineNumber;
  }

  /** A list of the codemod parameters that are involved in this change. */
  public List<CodeTFParameter> getParameters() {
    return parameters;
  }

  /**
   * A list of the dependencies that are required to be added to the project in order to support
   * this change.
   */
  public List<DependencyGAV> getDependenciesNeeded() {
    return dependenciesNeeded;
  }

  /**
   * A description of the change that is being made. If not provided, the default description will
   * be used.
   */
  public Optional<String> getDescription() {
    return Optional.ofNullable(description);
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

  public static CodemodChange from(final int line, final FixedFinding finding) {
    return new CodemodChange(line, finding);
  }

  /**
   * A {@link }
   */
  public static CodemodChange from(
      final int line, final Parameter parameter, final String valueUsed) {
    return new CodemodChange(line, parameter, valueUsed);
  }

  public static CodemodChange from(final int line, final String description) {
    return new CodemodChange(line, description);
  }

  @Override
  public String toString() {
    return "CodemodChange{"
        + "lineNumber="
        + lineNumber
        + ", dependenciesNeeded="
        + dependenciesNeeded
        + ", parameters="
        + parameters
        + ", description="
        + description
        + '}';
  }
}
