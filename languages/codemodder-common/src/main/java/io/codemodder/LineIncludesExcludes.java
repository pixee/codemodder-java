package io.codemodder;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * For a given file, this type provides an API for callers to understand if certain line numbers are
 * allowed.
 */
public interface LineIncludesExcludes {

  /** Return true if the include/exclude rules allow changes to this line. */
  boolean matches(int line);

  class MatchesEverything implements LineIncludesExcludes {
    @Override
    public boolean matches(int line) {
      return true;
    }
  }

  /** Given a set of lines to include, determine if we should allow changes to this line. */
  class IncludeBasedLineIncludesExcludes implements LineIncludesExcludes {

    private final Set<Integer> includedLines;

    private IncludeBasedLineIncludesExcludes(final Set<Integer> includedLines) {
      this.includedLines = Collections.unmodifiableSet(Objects.requireNonNull(includedLines));
    }

    @Override
    public boolean matches(int line) {
      return includedLines.contains(line);
    }
  }

  /** Given a set of lines to exclude, determine if we should allow changes to this line. */
  class ExcludeBasedLineIncludesExcludes implements LineIncludesExcludes {

    private final Set<Integer> excludedLines;

    private ExcludeBasedLineIncludesExcludes(final Set<Integer> excludedLines) {
      this.excludedLines = Collections.unmodifiableSet(Objects.requireNonNull(excludedLines));
    }

    @Override
    public boolean matches(int line) {
      return !excludedLines.contains(line);
    }
  }

  static LineIncludesExcludes fromIncludedLines(final Set<Integer> allowedLines) {
    return new IncludeBasedLineIncludesExcludes(allowedLines);
  }

  static LineIncludesExcludes fromExcludedLines(final Set<Integer> allowedLines) {
    return new ExcludeBasedLineIncludesExcludes(allowedLines);
  }
}
