package io.pixee.codefixer.java;

import java.io.File;
import java.io.IOException;

/** This type is used in include/exclude logic for matching paths. */
final class PathMatcher {

  private final String pathPrefix;
  private final Integer line;

  PathMatcher(final File repositoryRoot, final String path, final Integer line) {
    try {
      this.pathPrefix = new File(repositoryRoot, path).getCanonicalPath();
    } catch (IOException e) {
      throw new IllegalArgumentException("couldn't get canonical path", e);
    }
    this.line = line;
  }

  /** Return if this path matcher matches the given file. */
  boolean matches(final File file) {
    return file.getAbsolutePath().startsWith(pathPrefix);
  }

  /**
   * Return true if this instance has a longer include/exclude than another, and thus overrules the
   * previous.
   */
  boolean hasLongerPathThan(final PathMatcher matcher) {
    return pathPrefix.length() > matcher.pathPrefix.length();
  }

  boolean targetsFileExactly(final File file) {
    try {
      return file.getCanonicalPath().equals(this.pathPrefix);
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
}
