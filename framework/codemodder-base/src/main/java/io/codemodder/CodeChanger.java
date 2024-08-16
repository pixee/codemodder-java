package io.codemodder;

import io.codemodder.codetf.CodeTFReference;
import java.nio.file.Path;
import java.util.List;

/** The base of a codemod type. */
public interface CodeChanger {

  /** The headline for this codemod's changes. */
  String getSummary();

  /** A deep description of what this codemod's changes. */
  String getDescription();

  /**
   * A list of references for further reading on the issues this codemod addresses or other
   * supplementary information.
   */
  List<CodeTFReference> getReferences();

  /** A description of an individual change made by this codemod. */
  String getIndividualChangeDescription(final Path filePath, final CodemodChange change);

  /**
   * A list of paths patterns requested or rejected by the codemod. Those patterns are treated as
   * relative to the repository root. These patterns should follow the {@link
   * java.nio.file.PathMatcher} specification. These patterns can be overridden by global patterns.
   */
  default IncludesExcludesPattern getIncludesExcludesPattern() {
    return IncludesExcludesPattern.getAnyMatcher();
  }

  /**
   * A predicate which dictates if the file should be inspected by the codemod. This cannot be
   * overridden and should always pass before executing the codemod.
   */
  boolean supports(final Path file);

  /**
   * A lifecycle event that is called before any files are processed. This is a good place to short
   * circuit if you don't have the necessary resources (e.g., SARIF).
   */
  default boolean shouldRun() {
    return true;
  }
}
