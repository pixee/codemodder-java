package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** A type that plugins can implement to provide a project management functions to codemods. */
public interface ProjectProvider {

  /**
   * Given the file edited during codemod execution, attempt to update the dependencies in the
   * project to allow referencing new types.
   *
   * @param projectDir the root of the project
   * @param file the file that was edited and caused the dependency updates
   * @param remainingFileDependencies the dependencies that were requested but not yet added
   */
  DependencyUpdateResult updateDependencies(
      Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies) throws IOException;
}
