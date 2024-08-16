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

    private final Set<String> pathIncludes;
    private final Set<String> pathExcludes;

    public Default(final Set<String> pathIncludes, final Set<String> pathExcludes) {
      this.pathIncludes = pathIncludes;
      this.pathExcludes = pathExcludes;
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

  final class JavaMatcherSingleton {
    private JavaMatcherSingleton() {}

    private static IncludesExcludesPattern singleton;

    public static IncludesExcludesPattern getInstance() {
      if (singleton == null) {
        singleton =
            new IncludesExcludesPattern.Default(
                Set.of("**.[jJ][aA][vV][Aa]"), Collections.emptySet());
      }
      return singleton;
    }
  }

  final class AnySingleton {
    private AnySingleton() {}

    private static IncludesExcludesPattern singleton;

    public static IncludesExcludesPattern getInstance() {
      if (singleton == null) {
        singleton = new IncludesExcludesPattern.Default(Set.of("**"), Collections.emptySet());
      }
      return singleton;
    }
  }

  public static IncludesExcludesPattern getJavaMatcher() {
    return JavaMatcherSingleton.getInstance();
  }

  public static IncludesExcludesPattern getAnyMatcher() {
    return AnySingleton.getInstance();
  }
}
