package io.codemodder;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Represents the result of a dependency update operation. */
public interface DependencyUpdateResult {

  /** The list of dependencies that were successfully injected into the project. */
  List<FileDependency> injectedDependencies();

  /**
   * An updated model of the changed files in the project, after the dependency update operation.
   * This includes the changes that were passed in, as well as any changes that were made to the
   * project as a result of our updates.
   */
  Set<ChangedFile> updatedChanges();

  /** The set of files that we attempted to update, but failed. */
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
