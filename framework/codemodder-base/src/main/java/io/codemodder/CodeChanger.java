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
   * A lifecycle event that is called before any files are processed. This is a good place to short
   * circuit if you don't have the necessary resources (e.g., SARIF).
   */
  default boolean shouldRun() {
    return true;
  }
}
