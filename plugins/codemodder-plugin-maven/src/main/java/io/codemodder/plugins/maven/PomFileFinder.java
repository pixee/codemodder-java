package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** A type responsible for mapping files changed to their respective pom files. */
interface PomFileFinder {

  /**
   * Given a list of {@link DependencyGAV}s, build a map of pom files to the given dependencies that
   * we need to inject into them.
   *
   * @param projectDir the root directory of the project
   * @param file the file that requires the new dependency
   * @return an {@link Optional} representing the "nearest" pom.xml we found for that file
   */
  Optional<Path> findForFile(Path projectDir, Path file) throws IOException;
}
