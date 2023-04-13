package io.codemodder;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface DependencyUpdateResult {

  List<FileDependency> injectedDependencies();

  Set<ChangedFile> updatedChanges();

  Set<Path> erroredFiles();

  static DependencyUpdateResult create(
      final List<FileDependency> injectedDependencies,
      final Set<ChangedFile> updatedChanges,
      final Set<Path> erroredFiles) {
    return new Default(injectedDependencies, updatedChanges, erroredFiles);
  }

  class Default implements DependencyUpdateResult {

    private final List<FileDependency> injectedDependencies;
    private final Set<ChangedFile> updatedChanges;
    private final Set<Path> erroredFiles;

    private Default(
        final List<FileDependency> injectedDependencies,
        final Set<ChangedFile> updatedChanges,
        final Set<Path> erroredFiles) {
      this.injectedDependencies = Objects.requireNonNull(injectedDependencies);
      this.updatedChanges = Objects.requireNonNull(updatedChanges);
      this.erroredFiles = Objects.requireNonNull(erroredFiles);
    }

    @Override
    public List<FileDependency> injectedDependencies() {
      return injectedDependencies;
    }

    @Override
    public Set<ChangedFile> updatedChanges() {
      return updatedChanges;
    }

    @Override
    public Set<Path> erroredFiles() {
      return erroredFiles;
    }
  }
}
