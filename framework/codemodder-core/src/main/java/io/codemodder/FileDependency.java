package io.codemodder;

import java.nio.file.Path;
import java.util.List;

/** Represents a file that has dependencies on artifacts. */
public interface FileDependency {

  /** The full path to the file. */
  Path file();

  /**
   * The list of dependencies that this file has, but haven't been added to the dependency manager.
   */
  List<DependencyGAV> dependencies();

  static FileDependency create(final Path file, final List<DependencyGAV> dependencies) {
    return new FileDependency() {
      @Override
      public Path file() {
        return file;
      }

      @Override
      public List<DependencyGAV> dependencies() {
        return dependencies;
      }
    };
  }
}
