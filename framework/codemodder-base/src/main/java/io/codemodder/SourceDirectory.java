package io.codemodder;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/** Represent a normally-structured Java source code directory and all its files underneath it. */
public interface SourceDirectory extends Comparable<SourceDirectory> {

  /** The filesystem path of the directory. */
  Path path();

  /** The full path of all the source code files within this directory. */
  List<String> files();

  static SourceDirectory createDefault(final Path path, final List<String> files) {
    return new DefaultSourceDirectory(path, files);
  }

  class DefaultSourceDirectory implements SourceDirectory {

    private final List<String> files;
    private final Path sourceDirectoryPath;

    DefaultSourceDirectory(final Path sourceDirectoryPath, final List<String> files) {
      this.sourceDirectoryPath = requireNonNull(sourceDirectoryPath);
      this.files = requireNonNull(files);
    }

    @Override
    public Path path() {
      return sourceDirectoryPath;
    }

    @Override
    public List<String> files() {
      return files;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final DefaultSourceDirectory that = (DefaultSourceDirectory) o;
      return files.equals(that.files) && sourceDirectoryPath.equals(that.sourceDirectoryPath);
    }

    @Override
    public int hashCode() {
      return Objects.hash(files, sourceDirectoryPath);
    }

    @Override
    public String toString() {
      return "DefaultSourceDirectory{"
          + "files="
          + files
          + ", sourceDirectoryPath='"
          + sourceDirectoryPath
          + '\''
          + '}';
    }

    @Override
    public int compareTo(@NotNull SourceDirectory other) {
      return this.path().toString().compareTo(other.path().toString());
    }
  }
}
