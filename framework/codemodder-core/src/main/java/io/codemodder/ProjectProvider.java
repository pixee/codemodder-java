package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** A type that plugins can implement to provide a project management functions to codemods. */
public interface ProjectProvider {

  /**
   * Given the file edited during codemod execution, attempt to update the dependencies in the
   * project to allow referencing new types.
   */
  DependencyUpdateResult updateDependencies(
      Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies) throws IOException;
}
