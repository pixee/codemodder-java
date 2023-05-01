package io.codemodder;

import io.codemodder.codetf.CodeTFChangesetEntry;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class DefaultDependencyUpdateResult implements DependencyUpdateResult {

  private final List<DependencyGAV> injectedDependencies;
  private final Set<CodeTFChangesetEntry> updatedChanges;
  private final Set<Path> erroredFiles;

  DefaultDependencyUpdateResult(
      final List<DependencyGAV> injectedDependencies,
      final Set<CodeTFChangesetEntry> updatedChanges,
      final Set<Path> erroredFiles) {
    this.injectedDependencies = Objects.requireNonNull(injectedDependencies);
    this.updatedChanges = Objects.requireNonNull(updatedChanges);
    this.erroredFiles = Objects.requireNonNull(erroredFiles);
  }

  @Override
  public List<DependencyGAV> injectedPackages() {
    return injectedDependencies;
  }

  @Override
  public Set<CodeTFChangesetEntry> packageChanges() {
    return updatedChanges;
  }

  @Override
  public Set<Path> erroredFiles() {
    return erroredFiles;
  }
}
