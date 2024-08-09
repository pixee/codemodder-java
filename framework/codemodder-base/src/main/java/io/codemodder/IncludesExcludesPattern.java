package io.codemodder;

import java.nio.file.Path;
import java.util.*;

/** Holds includes and excludes patterns for files */
public interface IncludesExcludesPattern {

  /**
   * Returns an IncludesExcludes file matcher that matches files following the patterns against a
   * root folder.
   */
  public IncludesExcludes getRootedMatcher(final Path root);

  class Default implements IncludesExcludesPattern {

    private final List<String> pathIncludes;
    private final List<String> pathExcludes;

    public Default(final List<String> pathIncludes, final List<String> pathExcludes) {
      this.pathIncludes = pathIncludes;
      this.pathExcludes = pathExcludes;
    }

    @Override
    public String toString() {
      return "Includes: " + pathIncludes + "\nExcludes: " + pathExcludes;
    }

    @Override
    public IncludesExcludes getRootedMatcher(final Path root) {
      return IncludesExcludes.withSettings(root.toFile(), pathIncludes, pathExcludes);
    }
  }

  public static IncludesExcludesPattern getJavaMatcher() {
    return new Default(List.of("**.java"), List.of());
  }

  public static IncludesExcludesPattern getAnyMatcher() {
    return new Default(List.of("**"), List.of());
  }
}
