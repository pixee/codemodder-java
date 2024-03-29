package io.codemodder.javaparser;

import io.codemodder.DependencyGAV;
import java.util.List;
import java.util.Objects;

final class DefaultChangesResult implements ChangesResult {
  
  private final boolean changesApplied;
  private final List<DependencyGAV> dependenciesRequired;

  DefaultChangesResult(
      final boolean changesApplied, final List<DependencyGAV> dependenciesRequired) {
    this.changesApplied = changesApplied;
    this.dependenciesRequired = Objects.requireNonNull(dependenciesRequired);
  }

  @Override
  public boolean areChangesApplied() {
    return changesApplied;
  }

  @Override
  public List<DependencyGAV> getDependenciesRequired() {
    return dependenciesRequired;
  }
}
