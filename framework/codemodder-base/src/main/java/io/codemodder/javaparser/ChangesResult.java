package io.codemodder.javaparser;

import io.codemodder.DependencyGAV;
import java.util.List;

/** Represents the result of changes made during parsing. */
public interface ChangesResult {

  /** Returns true if changes were applied. */
  boolean areChangesApplied();

  List<DependencyGAV> getDependenciesRequired();

  ChangesResult noChanges = new DefaultChangesResult(false, List.of());
  ChangesResult changesApplied = new DefaultChangesResult(true, List.of());

  static ChangesResult changesAppliedWith(List<DependencyGAV> dependenciesRequired) {
    if (dependenciesRequired == null || dependenciesRequired.isEmpty()) {
      throw new IllegalArgumentException("Dependencies cannot be empty");
    }
    return new DefaultChangesResult(true, dependenciesRequired);
  }
}
