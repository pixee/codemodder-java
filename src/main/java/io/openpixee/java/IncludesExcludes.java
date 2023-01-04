package io.openpixee.java;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.*;

/** This is the main interaction point with types for detecting if a path should be included. */
public interface IncludesExcludes {

  /** Do we have any includes that include this file? */
  boolean shouldInspect(File file);

  /** Do we have any includes that match the file and line number? */
  LineIncludesExcludes getIncludesExcludesForFile(File file);

  class Default implements IncludesExcludes {

    private final List<PathMatcher> pathIncludes;
    private final List<PathMatcher> pathExcludes;

    public Default(final List<PathMatcher> pathIncludes, final List<PathMatcher> pathExcludes) {
      this.pathIncludes = pathIncludes;
      this.pathExcludes = pathExcludes;
    }

    @Override
    public boolean shouldInspect(final File file) {
      if (!pathIncludes.isEmpty()) {
        for (PathMatcher pathInclude : pathIncludes) {
          if (pathInclude.matches(file)) {
            // unless there is a deeper exclude, we match
            for (PathMatcher pathExclude : pathExcludes) {
              if (pathExclude.matches(file) && pathExclude.hasLongerPathThan(pathInclude)) {
                return false;
              }
            }
            return true;
          }
        }
        return false;
      }

      for (PathMatcher pathExclude : pathExcludes) {
        if (!pathExclude.targetsLine() && pathExclude.matches(file)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public LineIncludesExcludes getIncludesExcludesForFile(final File file) {
      final Set<Integer> allowedLines = new HashSet<>();
      for (PathMatcher pathInclude : pathIncludes) {
        if (pathInclude.targetsLine()) {
          if (pathInclude.targetsFileExactly(file)) {
            allowedLines.add(Objects.requireNonNull(pathInclude.line()));
          }
        }
      }

      final Set<Integer> disallowedLines = new HashSet<>();
      for (PathMatcher pathExclude : pathExcludes) {
        if (pathExclude.targetsLine()) {
          if (pathExclude.targetsFileExactly(file)) {
            disallowedLines.add(Objects.requireNonNull(pathExclude.line()));
          }
        }
      }

      if (!allowedLines.isEmpty() && !disallowedLines.isEmpty()) {
        throw new IllegalArgumentException(
            "can't have both include and exclude targetining individual lines for a file");
      }

      if (allowedLines.isEmpty() && disallowedLines.isEmpty()) {
        return new LineIncludesExcludes.MatchesEverything();
      }

      if (!allowedLines.isEmpty()) {
        return LineIncludesExcludes.fromIncludedLines(allowedLines);
      }
      return LineIncludesExcludes.fromExcludedLines(disallowedLines);
    }

    @Override
    public String toString() {
      return "Includes: " + pathIncludes + "\nExcludes: " + pathExcludes;
    }
  }

  /**
   * Create an {@link IncludesExcludes} based on the configuration we're using -- the include
   * patterns and exclude patterns.
   */
  static IncludesExcludes fromConfiguration(
      final File repositoryRoot,
      final List<String> includePatterns,
      final List<String> excludePatterns) {

    if (noPatternsSpecified(includePatterns, excludePatterns)) {
      return new MatchesEverything();
    }
    List<PathMatcher> pathIncludes = new ArrayList<>();
    List<PathMatcher> pathExcludes = new ArrayList<>();
    for (String includePattern : includePatterns) {
      pathIncludes.add(parsePattern(repositoryRoot, includePattern));
    }

    for (String excludePattern : excludePatterns) {
      pathExcludes.add(parsePattern(repositoryRoot, excludePattern));
    }

    return new Default(pathIncludes, pathExcludes);
  }

  @VisibleForTesting
  static PathMatcher parsePattern(final File repositoryRoot, String pattern) {
    // validate and sanitize
    Objects.requireNonNull(pattern);
    pattern = pattern.trim();

    // determine if this targets a line
    int lineSeparatorIndex = pattern.indexOf(':');

    final Integer line;
    final String pathPrefix;

    // if it targets a line, get the path and line separately
    if (lineSeparatorIndex != -1) {
      pathPrefix = pattern.substring(0, lineSeparatorIndex);
      line = Integer.parseInt(pattern.substring(lineSeparatorIndex + 1));
    } else {
      pathPrefix = pattern;
      line = null;
    }
    try {
      return new PathMatcher(repositoryRoot, pathPrefix, line);
    } catch (Exception e) {
      throw new IllegalArgumentException("couldn't get canonical path", e);
    }
  }

  private static boolean noPatternsSpecified(
      final List<String> includePatterns, final List<String> excludePatterns) {
    return (includePatterns == null || includePatterns.isEmpty())
        && (excludePatterns == null || excludePatterns.isEmpty());
  }

  class MatchesEverything implements IncludesExcludes {
    @Override
    public boolean shouldInspect(final File file) {
      return true;
    }

    @Override
    public LineIncludesExcludes getIncludesExcludesForFile(final File file) {
      return new LineIncludesExcludes.MatchesEverything();
    }
  }
}
