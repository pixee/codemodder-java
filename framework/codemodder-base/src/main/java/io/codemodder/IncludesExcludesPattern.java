package io.codemodder;

import java.nio.file.Path;
import java.util.*;

/** Holds includes and excludes patterns for files */
public interface IncludesExcludesPattern {

  /**
   * Returns an IncludesExcludes file matcher that matches files following the patterns specified
   * patterns. The patterns must follow the java's {@link java.nio.file.PathMatcher} specification
   * and are treated as relative paths with regard to the repository root.
   */
  IncludesExcludes getRootedMatcher(final Path root);

  class Default implements IncludesExcludesPattern {

    private final Set<String> pathIncludes;
    private final Set<String> pathExcludes;

    public Default(final Set<String> pathIncludes, final Set<String> pathExcludes) {
      this.pathIncludes = Objects.requireNonNull(pathIncludes);
      this.pathExcludes = Objects.requireNonNull(pathExcludes);
    }

    @Override
    public String toString() {
      return "Includes: " + pathIncludes + "\nExcludes: " + pathExcludes;
    }

    @Override
    public IncludesExcludes getRootedMatcher(final Path root) {
      return IncludesExcludes.withSettings(
          root.toFile(), pathIncludes.stream().toList(), pathExcludes.stream().toList());
    }
  }

  /** An includes/excludes pattern that matches java files. */
  final class JavaMatcherSingleton {
    private static IncludesExcludesPattern singleton;

    private JavaMatcherSingleton() {}

    static IncludesExcludesPattern getInstance() {
      if (singleton == null) {
        singleton =
            new IncludesExcludesPattern.Default(
                Set.of("**.[jJ][aA][vV][Aa]"), Collections.emptySet());
      }
      return singleton;
    }
  }

  /** An includes/excludes pattern that matches any files. */
  final class AnySingleton {
    private static IncludesExcludesPattern singleton;

    private AnySingleton() {}

    static IncludesExcludesPattern getInstance() {
      if (singleton == null) {
        singleton = new IncludesExcludesPattern.Default(Set.of("**"), Collections.emptySet());
      }
      return singleton;
    }
  }

  /** Returns an includes/excludes pattern that matches any java files. */
  static IncludesExcludesPattern getJavaMatcher() {
    return JavaMatcherSingleton.getInstance();
  }

  /** Returns an includes/excludes pattern that matches any files. */
  static IncludesExcludesPattern getAnyMatcher() {
    return AnySingleton.getInstance();
  }
}
