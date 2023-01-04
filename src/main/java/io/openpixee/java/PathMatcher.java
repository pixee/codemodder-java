package io.openpixee.java;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/** This type is used in include/exclude logic for matching paths. */
final class PathMatcher {

  private final Path pathPrefix;
  private final Integer line;

  PathMatcher(final File repositoryRoot, final String path, final Integer line) {
    try {
      this.pathPrefix =
          new File(repositoryRoot, path).getCanonicalFile().getAbsoluteFile().toPath();
    } catch (IOException e) {
      throw new IllegalArgumentException("couldn't get canonical path", e);
    }
    this.line = line;
  }

  /** Return if this path matcher matches the given file. */
  boolean matches(final File file) {
    try {
      return file.getCanonicalFile().getAbsoluteFile().toPath().startsWith(pathPrefix);
    } catch (IOException e) {
      throw new IllegalArgumentException("couldn't get canonical path", e);
    }
  }

  /**
   * Return true if this instance has a longer include/exclude than another, and thus overrules the
   * previous.
   */
  boolean hasLongerPathThan(final PathMatcher matcher) {
    return !pathPrefix.equals(matcher.pathPrefix) && pathPrefix.startsWith(matcher.pathPrefix);
  }

  boolean targetsFileExactly(final File file) {
    try {
      return file.getCanonicalFile().getAbsoluteFile().toPath().equals(this.pathPrefix);
    } catch (IOException e) {
      throw new IllegalArgumentException("couldn't get canonical path", e);
    }
  }

  Integer line() {
    return line;
  }

  boolean targetsLine() {
    return line != null;
  }

  @Override
  public String toString() {
    return this.pathPrefix + ":" + this.line;
  }
}
