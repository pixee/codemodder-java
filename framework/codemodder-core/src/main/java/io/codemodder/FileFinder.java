package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Responsible for finding all files in a directory that match the includes and excludes. */
interface FileFinder {

  /**
   * Finds all files in a directory that match the includes and excludes. It's worth noting that the
   * returned list will contain files that should only be inspected on a per-line basis. This must
   * be taken into account downstream.
   *
   * @param projectDirectory the directory to search
   * @param includesExcludes the includes and excludes
   * @return a list of files that match the includes and excludes
   */
  List<Path> findFiles(final Path projectDirectory, final IncludesExcludes includesExcludes)
      throws IOException;
}
