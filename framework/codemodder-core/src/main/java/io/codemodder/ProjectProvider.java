package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** A type that plugins can implement to provide a project management functions to codemods. */
public interface ProjectProvider {

  /**
   * Given the {@link ChangedFile}s created during codemod execution, attempt to update the
   * dependencies in the project. This may result in an updated {@link ChangedFile} set.
   */
  DependencyUpdateResult updateDependencies(
      Path projectDir,
      Set<ChangedFile> changedFiles,
      List<FileDependency> remainingFileDependencies)
      throws IOException;
}
