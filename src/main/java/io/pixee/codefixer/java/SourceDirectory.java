package io.pixee.codefixer.java;

import static java.util.Objects.requireNonNull;

import java.util.List;

/** Represent a normally-structured Java source code directory and all its files underneath it. */
public interface SourceDirectory {

  /** The filesystem path of the directory. */
  String path();

  /** The full path of all the source code files within this directory. */
  List<String> files();

  static SourceDirectory createDefault(final String path, final List<String> files) {
    return new DefaultSourceDirectory(path, files);
  }

  class DefaultSourceDirectory implements SourceDirectory {

    private final List<String> files;
    private final String sourceDirectoryPath;

    DefaultSourceDirectory(final String sourceDirectoryPath, final List<String> files) {
      this.sourceDirectoryPath = requireNonNull(sourceDirectoryPath);
      this.files = requireNonNull(files);
    }

    @Override
    public String path() {
      return sourceDirectoryPath;
    }

    @Override
    public List<String> files() {
      return files;
    }
  }
}
