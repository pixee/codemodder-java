package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Responsible for finding all files in a directory that match the includes and excludes. */
interface FileFinder {

  /**
   * Finds all files in directories that match the includes and excludes. It's worth noting that the
   * returned list will contain files that should only be inspected on a per-line basis. This must
   * be taken into account downstream. The files should be sorted according to a system-dependent
   * method.
   *
   * @param sourceDirectories the directories to search
   * @param includesExcludes the includes and excludes
   * @return a list of files that match the includes and excludes
   */
  List<Path> findFiles(List<SourceDirectory> sourceDirectories, IncludesExcludes includesExcludes)
      throws IOException;
}
