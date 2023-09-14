package io.codemodder;

import io.codemodder.codetf.CodeTFChangesetEntry;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Represents the result of a dependency update operation. */
public interface DependencyUpdateResult {

  /** The list of dependencies that were successfully injected into the project. */
  List<DependencyGAV> injectedPackages();

  /** The list of dependencies that were already available to the given file. */
  List<DependencyGAV> skippedPackages();

  /**
   * An updated model of the changed files in the project, after the dependency update operation.
   * This includes the changes that were passed in, as well as any changes that were made to the
   * project as a result of our updates.
   */
  List<CodeTFChangesetEntry> packageChanges();

  /** The set of files that we attempted to update, but failed. */
  Set<Path> erroredFiles();

  /** An update reporting no actions and no errors. */
  DependencyUpdateResult EMPTY_UPDATE = create(List.of(), List.of(), List.of(), Set.of());

  static DependencyUpdateResult create(
      final List<DependencyGAV> injectedDependencies,
      final List<DependencyGAV> skippedPackages,
      final List<CodeTFChangesetEntry> updatedChanges,
      final Set<Path> erroredFiles) {
    return new DefaultDependencyUpdateResult(
        injectedDependencies, skippedPackages, updatedChanges, erroredFiles);
  }
}
