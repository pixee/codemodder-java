package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Holds a code directory (e.g., a repository root). */
public interface CodeDirectory {

  /** The filesystem directory path we are running against. */
  Path asPath();

  /**
   * Find a file with the given trailing path. This is useful for situations in which you only know
   * the last part of the path for a file within the project.
   */
  Optional<Path> findFilesWithTrailingPath(final String path) throws IOException;

  static CodeDirectory from(final Path projectDir) {
    return new DefaultCodeDirectory(projectDir);
  }
}
