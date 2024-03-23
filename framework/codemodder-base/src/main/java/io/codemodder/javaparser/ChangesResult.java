package io.codemodder.javaparser;

import io.codemodder.DependencyGAV;
import java.util.List;

/** Represents the result of changes made during parsing. */
public class ChangesResult {

  private final boolean changesApplied;
  private final List<DependencyGAV> dependenciesRequired;

  private ChangesResult(
      final boolean changesApplied, final List<DependencyGAV> dependenciesRequired) {
    this.changesApplied = changesApplied;
    this.dependenciesRequired = dependenciesRequired;
  }

  /**
   * Creates a ChangesResult indicating that no changes were applied.
   *
   * @return ChangesResult instance with no changes applied.
   */
  public static ChangesResult noChanges() {
    return new ChangesResult(false, List.of());
  }

  /**
   * Creates a ChangesResult indicating that changes were applied.
   *
   * @return ChangesResult instance with changes applied.
   */
  public static ChangesResult changesApplied() {
    return new ChangesResult(true, List.of());
  }

  /**
   * Creates a ChangesResult indicating that changes were applied and additional dependencies are
   * required.
   *
   * @param dependenciesRequired List of dependencies required due to changes applied.
   * @return ChangesResult instance with changes applied and additional dependencies required.
   */
  public static ChangesResult changesApplied(final List<DependencyGAV> dependenciesRequired) {
    return new ChangesResult(true, dependenciesRequired);
  }

  /**
   * Checks if changes were applied.
   *
   * @return True if changes were applied, false otherwise.
   */
  public boolean areChangesApplied() {
    return changesApplied;
  }

  /**
   * Retrieves the list of dependencies required.
   *
   * @return List of dependencies required due to changes applied.
   */
  public List<DependencyGAV> getDependenciesRequired() {
    return dependenciesRequired;
  }
}
