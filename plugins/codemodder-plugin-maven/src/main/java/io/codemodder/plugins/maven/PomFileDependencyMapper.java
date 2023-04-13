package io.codemodder.plugins.maven;

import io.codemodder.FileDependency;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** A type responsible for mapping files changed to their respective pom files. */
interface PomFileDependencyMapper {

  /**
   * Given a list of {@link FileDependency}s, build a map of pom files to the given dependencies
   * that we need to inject into them.
   *
   * @param projectDir the root directory of the project
   * @param remainingFileDependencies the files and their required dependencies
   * @return a map of pom files to the dependencies that need to be injected into them
   */
  Map<Path, List<FileDependency>> build(
      Path projectDir, List<FileDependency> remainingFileDependencies) throws IOException;
}
